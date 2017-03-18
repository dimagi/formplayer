package parsers;

import database.models.EntityStorageCache;
import database.models.FormplayerCaseIndexTable;
import org.commcare.cases.model.Case;
import org.commcare.xml.bulk.BulkProcessingCaseXmlParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;
import sandbox.SqliteIndexedStorageUtility;
import sandbox.UserSqlSandbox;

import java.io.IOException;
import java.util.*;

/**
 * A bulk processing parser for Formplayer. Provides superior performance when
 * processing high case loads during syncing and/or processing.
 *
 * @author ctsims
 */
public class FormplayerBulkCaseXmlParser extends BulkProcessingCaseXmlParser {
    private final EntityStorageCache mEntityCache;
    private final FormplayerCaseIndexTable mCaseIndexTable;
    private final SqliteIndexedStorageUtility<Case> storage;

    public FormplayerBulkCaseXmlParser(KXmlParser parser,
                                    UserSqlSandbox sandbox) {
        super(parser);
        mEntityCache = new EntityStorageCache("entitycase", sandbox);
        mCaseIndexTable = new FormplayerCaseIndexTable(sandbox);
        this.storage = sandbox.getCaseStorage();
    }

    @Override
    protected Case buildCase(String name, String typeId) {
        return new Case(name, typeId);
    }

    @Override
    protected void performBulkRead(Set<String> currentBulkReadSet, Map<String, Case> currentOperatingSet) throws InvalidStructureException, IOException, XmlPullParserException {
        for (Case c : storage.getBulkRecordsForIndex(Case.INDEX_CASE_ID, currentBulkReadSet)) {
            currentOperatingSet.put(c.getCaseId(), c);
        }
    }

    @Override
    protected void performBulkWrite(LinkedHashMap<String, Case> writeLog) throws IOException {
        ArrayList<Integer> recordIdsToWipe = new ArrayList<>();
        for (String caseId : writeLog.keySet()) {
            Case c = writeLog.get(caseId);
            storage.write(c);
            // Add the case's SQL record ID
            recordIdsToWipe.add(c.getID());
        }
        mEntityCache.invalidateCaches(recordIdsToWipe);
        mCaseIndexTable.clearCaseIndices(recordIdsToWipe);
        for (String cid : writeLog.keySet()) {
            Case c = writeLog.get(cid);
            mCaseIndexTable.indexCase(c);
        }
    }
}