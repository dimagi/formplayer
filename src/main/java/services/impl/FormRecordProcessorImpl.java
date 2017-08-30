package services.impl;

import com.timgroup.statsd.StatsDClient;
import database.models.FormplayerCaseIndexTable;
import engine.FormplayerTransactionParserFactory;
import hq.CaseAPIs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.ledger.LedgerPurgeFilter;
import org.commcare.cases.model.Case;
import org.commcare.cases.util.CasePurgeFilter;
import org.commcare.core.process.XmlFormRecordProcessor;
import org.commcare.core.sandbox.SandboxUtils;
import org.javarosa.core.model.User;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.xmlpull.v1.XmlPullParserException;
import sandbox.SqliteIndexedStorageUtility;
import sandbox.UserSqlSandbox;
import services.RecordProcessorService;
import services.RestoreFactory;
import util.Constants;
import util.PropertyUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * * Convenience methods, mostly for Touchforms so we don't have to deal with Java IO
 * in Jython which is terrible
 * <p>
 * Created by wpride1 on 8/20/15.
 */
public class FormRecordProcessorImpl extends XmlFormRecordProcessor implements RecordProcessorService {

    @Autowired
    protected StatsDClient datadogStatsDClient;

    @Autowired
    protected RestoreFactory restoreFactory;

    private final Log log = LogFactory.getLog(FormRecordProcessorImpl.class);

    // This function will only wipe user DBs when they have expired, otherwise will incremental sync
    public UserSqlSandbox performSync(RestoreFactory restoreFactory) throws Exception {
        if (restoreFactory.isRestoreXmlExpired()) {
            restoreFactory.getSQLiteDB().deleteDatabaseFile();
        }
        // Create parent dirs if needed
        if(restoreFactory.getSqlSandbox().getLoggedInUser() != null){
            restoreFactory.getSQLiteDB().createDatabaseFolder();
        }
        UserSqlSandbox sandbox = CaseAPIs.restoreUser(restoreFactory, restoreFactory.getRestoreXml());
        purgeCases(sandbox);
        return sandbox;
    }

    public void processXML(FormplayerTransactionParserFactory factory, String fileText)
            throws IOException, XmlPullParserException, UnfullfilledRequirementsException, InvalidStructureException {
        InputStream stream = new ByteArrayInputStream(fileText.getBytes("UTF-8"));
        process(stream, factory);
        if (factory.wereCaseIndexesDisrupted() && PropertyUtils.isAutoPurgeEnabled()) {
            purgeCases(factory.getSqlSandbox());
        }
    }

    /**
     * Perform a case purge against the logged in user with the logged in app in local storage.
     * This is coped almost directly from commcare-android's CaseUtils class.
     * TODO They should be unified
     *
     */
    public void purgeCases(UserSqlSandbox sandbox) {
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

        int removedCaseCount;
        int removedLedgers;
        SqliteIndexedStorageUtility<Case> storage = sandbox.getCaseStorage();
        CasePurgeFilter filter = new CasePurgeFilter(storage, owners);
        if (filter.invalidEdgesWereRemoved()) {
            log.error("An invalid edge was created in the internal " +
                    "case DAG of a case purge filter, meaning that at least 1 case on the " +
                    "device had an index into another case that no longer exists on the device");
            log.error("Case lists on the server and device" +
                    " were out of sync. The following cases were expected to be on the device, " +
                    "but were missing: " + filter.getMissingCasesString() + ". As a result, the " +
                    "following cases were also removed from the device: " + filter.getRemovedCasesString());
        }

        Vector<Integer> casesRemoved = storage.removeAll(filter);
        removedCaseCount = casesRemoved.size();
        FormplayerCaseIndexTable indexTable = new FormplayerCaseIndexTable(sandbox);
        for (int recordId : casesRemoved) {
            indexTable.clearCaseIndices(recordId);
        }


        SqliteIndexedStorageUtility<Ledger> stockStorage = sandbox.getLedgerStorage();
        LedgerPurgeFilter stockFilter = new LedgerPurgeFilter(stockStorage, storage);
        removedLedgers = stockStorage.removeAll(stockFilter).size();


        long taken = System.currentTimeMillis() - start;

        log.info(String.format(
                "Purged [%d Case, %d Ledger] records in %dms",
                removedCaseCount, removedLedgers, taken));
        datadogStatsDClient.recordExecutionTime(
                Constants.DATADOG_TIMINGS,
                taken,
                "domain:" + restoreFactory.getDomain(),
                "username" + restoreFactory.getWrappedUsername(),
                "request:" + "case-purge",
                "case-count" + removedCaseCount
        );
    }
}
