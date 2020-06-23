package org.commcare.formplayer.api.process;

import org.commcare.formplayer.database.models.FormplayerCaseIndexTable;
import org.commcare.formplayer.engine.FormplayerTransactionParserFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.ledger.LedgerPurgeFilter;
import org.commcare.cases.model.Case;
import org.commcare.cases.model.CaseIndex;
import org.commcare.cases.util.CasePurgeFilter;
import org.commcare.core.process.XmlFormRecordProcessor;
import org.commcare.core.sandbox.SandboxUtils;
import org.commcare.modern.util.Pair;
import org.commcare.util.LogTypes;
import org.javarosa.core.model.User;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.util.DAG;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;
import org.commcare.formplayer.sandbox.JdbcSqlStorageIterator;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.util.SimpleTimer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Vector;

/**
 * * Convenience methods, mostly for Touchforms so we don't have to deal with Java IO
 * in Jython which is terrible
 * <p>
 * Created by wpride1 on 8/20/15.
 */
public class FormRecordProcessorHelper extends XmlFormRecordProcessor {
    private static final Log log = LogFactory.getLog(FormRecordProcessorHelper.class);

    public static class TimingResult {
        private SimpleTimer purgeCasesTimer;

        private TimingResult(SimpleTimer purgeCasesTimer) {
            this.purgeCasesTimer = purgeCasesTimer;
        }

        public SimpleTimer getPurgeCasesTimer() {
            return purgeCasesTimer;
        }
    }

    public static TimingResult processXML(FormplayerTransactionParserFactory factory,
                                          String fileText,
                                          boolean autoPurgeEnabled) throws IOException, XmlPullParserException, UnfullfilledRequirementsException, InvalidStructureException {
        InputStream stream = new ByteArrayInputStream(fileText.getBytes("UTF-8"));
        process(stream, factory);
        SimpleTimer timer = new SimpleTimer();
        timer.start();
        if (factory.wereCaseIndexesDisrupted() && autoPurgeEnabled) {
            purgeCases(factory.getSqlSandbox());
        }
        timer.end();
        return new TimingResult(timer);
    }

    /**
     * Perform a case purge against the logged in user with the logged in app in local storage.
     * This is coped almost directly from commcare-android's CaseUtils class.
     * TODO They should be unified
     */
    public static void purgeCases(UserSqlSandbox sandbox) {
        long start = System.currentTimeMillis();
        //We need to determine if we're using ownership for purging. For right now, only in sync mode
        Vector<String> owners = new Vector<>();
        Vector<String> users = new Vector<>();
        for (IStorageIterator<User> userIterator = sandbox.getUserStorage().iterate(); userIterator.hasMore(); ) {
            String id = userIterator.nextRecord().getUniqueId();
            owners.addElement(id);
            users.addElement(id);
        }

        //Now add all of the relevant groups
        //TODO: Wow. This is.... kind of megasketch
        for (String userId : users) {
            DataInstance instance = SandboxUtils.loadFixture(sandbox, "user-groups", userId);
            if (instance == null) {
                continue;
            }
            EvaluationContext ec = new EvaluationContext(instance);
            for (TreeReference ref : ec.expandReference(XPathReference.getPathExpr("/groups/group/@id").getReference())) {
                AbstractTreeElement<AbstractTreeElement> idelement = ec.resolveReference(ref);
                if (idelement.getValue() != null) {
                    owners.addElement(idelement.getValue().uncast().getString());
                }
            }
        }

        int removedCaseCount = -1;
        int removedLedgers = -1;

        SqlStorage<Case> storage = sandbox.getCaseStorage();
        DAG<String, int[], String> fullCaseGraph = getFullCaseGraph(storage, new FormplayerCaseIndexTable(sandbox), owners);

        CasePurgeFilter filter = new CasePurgeFilter(fullCaseGraph);
        if (filter.invalidEdgesWereRemoved()) {
            Logger.log(LogTypes.SOFT_ASSERT, "An invalid edge was created in the internal " +
                    "case DAG of a case purge filter, meaning that at least 1 case on the " +
                    "device had an index into another case that no longer exists on the device");
            Logger.log(LogTypes.TYPE_ERROR_ASSERTION, "Case lists on the server and device" +
                    " were out of sync. The following cases were expected to be on the device, " +
                    "but were missing: " + filter.getMissingCasesString() + ". As a result, the " +
                    "following cases were also removed from the device: " + filter.getRemovedCasesString());
        }

        Vector<Integer> casesRemoved = storage.removeAll(filter.getCasesToRemove());
        removedCaseCount = casesRemoved.size();

        FormplayerCaseIndexTable indexTable = new FormplayerCaseIndexTable(sandbox);
        for (int recordId : casesRemoved) {
            indexTable.clearCaseIndices(recordId);
        }


        SqlStorage<Ledger> stockStorage = sandbox.getLedgerStorage();
        LedgerPurgeFilter stockFilter = new LedgerPurgeFilter(stockStorage, storage);
        removedLedgers = stockStorage.removeAll(stockFilter).size();


        long taken = System.currentTimeMillis() - start;
        log.info(String.format(
                "Purged [%d Case, %d Ledger] records in %dms",
                removedCaseCount, removedLedgers, taken));

    }

    public static DAG<String, int[], String> getFullCaseGraph(SqlStorage<Case> caseStorage,
                                                              FormplayerCaseIndexTable indexTable,
                                                              Vector<String> owners) {
        DAG<String, int[], String> caseGraph = new DAG<>();
        Vector<Pair<String, String>> indexHolder = new Vector<>();

        HashMap<Integer, Vector<Pair<String, String>>> caseIndexMap = indexTable.getCaseIndexMap();

        // Pass 1: Create a DAG which contains all of the cases on the phone as nodes, and has a
        // directed edge for each index (from the 'child' case pointing to the 'parent' case) with
        // the appropriate relationship tagged
        for (JdbcSqlStorageIterator<Case> i = caseStorage.iterate(true,
                new String[]{Case.INDEX_OWNER_ID, Case.INDEX_CASE_STATUS, Case.INDEX_CASE_ID}); i.hasMore(); ) {

            String ownerId = i.peekIncludedMetadata(Case.INDEX_OWNER_ID);
            boolean closed = i.peekIncludedMetadata(Case.INDEX_CASE_STATUS).equals("closed");
            String caseID = i.peekIncludedMetadata(Case.INDEX_CASE_ID);
            int caseRecordId = i.nextID();


            boolean owned = true;
            if (owners != null) {
                owned = owners.contains(ownerId);
            }

            Vector<Pair<String, String>> indices = caseIndexMap.get(caseRecordId);

            if (indices != null) {
                // In order to deal with multiple indices pointing to the same case with different
                // relationships, we'll need to traverse once to eliminate any ambiguity
                for (Pair<String, String> index : indices) {
                    Pair<String, String> toReplace = null;
                    boolean skip = false;
                    for (Pair<String, String> existing : indexHolder) {
                        if (existing.first.equals(index.first)) {
                            if (existing.second.equals(CaseIndex.RELATIONSHIP_EXTENSION) && !index.second.equals(CaseIndex.RELATIONSHIP_EXTENSION)) {
                                toReplace = existing;
                            } else {
                                skip = true;
                            }
                            break;
                        }
                    }
                    if (toReplace != null) {
                        indexHolder.removeElement(toReplace);
                    }
                    if (!skip) {
                        indexHolder.addElement(index);
                    }
                }
            }
            int nodeStatus = 0;
            if (owned) {
                nodeStatus |= CasePurgeFilter.STATUS_OWNED;
            }

            if (!closed) {
                nodeStatus |= CasePurgeFilter.STATUS_OPEN;
            }

            if (owned && !closed) {
                nodeStatus |= CasePurgeFilter.STATUS_RELEVANT;
            }

            caseGraph.addNode(caseID, new int[]{nodeStatus, caseRecordId});

            for (Pair<String, String> index : indexHolder) {
                caseGraph.setEdge(caseID, index.first, index.second);
            }
            indexHolder.removeAllElements();
        }

        return caseGraph;
    }
}
