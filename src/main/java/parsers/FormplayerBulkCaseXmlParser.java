package org.commcare.xml;

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
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * A bulk processing parser for the android platform. Provides superior performance when
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
    protected void performBulkWrite(SortedMap<String, Case> writeLog) throws IOException {
        ArrayList<Integer> recordIdsToWipe = new ArrayList<>();
        for(Case c : writeLog.values()) {
            storage.write(c);
            recordIdsToWipe.add(c.getID());
            mCaseIndexTable.indexCase(c);
        }
        mEntityCache.invalidateCaches(recordIdsToWipe);
        mCaseIndexTable.clearCaseIndices(recordIdsToWipe);
        for(Case c : writeLog.values()) {
            mCaseIndexTable.indexCase(c);
        }
    }
}