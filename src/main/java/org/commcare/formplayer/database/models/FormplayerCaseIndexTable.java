package org.commcare.formplayer.database.models;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.cases.model.Case;
import org.commcare.cases.model.CaseIndex;
import org.commcare.cases.query.queryset.DualTableSingleMatchModelQuerySet;
import org.commcare.formplayer.postgresutil.PostgresDB;
import org.commcare.modern.database.DatabaseHelper;
import org.commcare.modern.database.DatabaseIndexingUtils;
import org.commcare.modern.database.TableBuilder;
import org.commcare.modern.engine.cases.CaseIndexTable;
import org.commcare.modern.util.Pair;
import org.commcare.formplayer.sandbox.SqlHelper;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.services.ConnectionHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author ctsims
 */
public class FormplayerCaseIndexTable implements CaseIndexTable {
    public static final String TABLE_NAME = "case_index_storage";

    private static final String COL_CASE_RECORD_ID = "case_rec_id";
    private static final String COL_INDEX_NAME = "name";
    private static final String COL_INDEX_TYPE = "type";
    private static final String COL_INDEX_TARGET = "target";
    private static final String COL_INDEX_RELATIONSHIP = "relationship";

    ConnectionHandler connectionHandler;

    private static final Log log = LogFactory.getLog(FormplayerCaseIndexTable.class);

    //TODO: We should do some synchronization to make it the case that nothing can hold
    //an object for the same cache at once and let us manage the lifecycle

    public FormplayerCaseIndexTable(ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
        execSQL(connectionHandler.getConnection(), getTableDefinition());
        createIndexes(connectionHandler.getConnection());
    }

    private static void execSQL(Connection connection, String query) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    log.debug("Exception closing prepared statement ", e);
                }
            }
        }
    }

    private static String getTableDefinition() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" +
                DatabaseHelper.ID_COL + " INTEGER PRIMARY KEY, " +
                COL_CASE_RECORD_ID + ", " +
                COL_INDEX_NAME + ", " +
                COL_INDEX_TYPE + ", " +
                COL_INDEX_RELATIONSHIP + ", " +
                COL_INDEX_TARGET +
                ")";
    }

    private static void createIndexes(Connection connection) {
        String recordFirstIndexId = "RECORD_NAME_ID_TARGET";
        String recordFirstIndex = COL_CASE_RECORD_ID + ", " + COL_INDEX_NAME + ", " + COL_INDEX_TARGET;
        execSQL(connection, DatabaseIndexingUtils.indexOnTableCommand(recordFirstIndexId, TABLE_NAME, recordFirstIndex));

        String typeFirstIndexId = "NAME_TARGET_RECORD";
        String typeFirstIndex = COL_INDEX_NAME + ", " + COL_CASE_RECORD_ID + ", " + COL_INDEX_TARGET;
        execSQL(connection, DatabaseIndexingUtils.indexOnTableCommand(typeFirstIndexId, TABLE_NAME, typeFirstIndex));
    }

    /**
     * Creates all indexes for this case.
     * TODO: this doesn't ensure any sort of uniquenes, you should wipe constraints first
     */
    public void indexCase(Case c) {
        for (CaseIndex ci : c.getIndices()) {
            HashMap<String, Object> contentValues = new HashMap<>();
            contentValues.put(COL_CASE_RECORD_ID, "" + c.getID());
            contentValues.put(COL_INDEX_NAME, ci.getName());
            contentValues.put(COL_INDEX_TYPE, ci.getTargetType());
            contentValues.put(COL_INDEX_TARGET, ci.getTarget());
            contentValues.put(COL_INDEX_RELATIONSHIP, ci.getRelationship());
            SqlHelper.basicInsert(connectionHandler.getConnection(), TABLE_NAME, contentValues, isPostgres(), getCurrentSchema());
        }
    }

    public void clearCaseIndices(Case c) {
        clearCaseIndices(c.getID());
    }

    public void clearCaseIndices(int recordId) {
        String recordIdString = String.valueOf(recordId);
        SqlHelper.deleteFromTableWhere(connectionHandler.getConnection(),
                TABLE_NAME,
                COL_CASE_RECORD_ID + "= CAST(? as INT)",
                recordIdString, isPostgres(), getCurrentSchema());
    }

    public void clearCaseIndices(Collection<Integer> idsToClear) {
        if (idsToClear.size() == 0) {
            return;
        }
        List<Pair<String, String[]>> whereParamList = TableBuilder.sqlList(idsToClear);
        for (Pair<String, String[]> whereParams : whereParamList) {
            SqlHelper.deleteFromTableWhere(connectionHandler.getConnection(),
                    TABLE_NAME,
                    COL_CASE_RECORD_ID + " IN " + whereParams.first,
                    whereParams.second, isPostgres(), getCurrentSchema());
        }
    }


    public HashMap<Integer,Vector<Pair<String, String>>> getCaseIndexMap() {
        String[] projection = new String[] {COL_CASE_RECORD_ID, COL_INDEX_TARGET, COL_INDEX_RELATIONSHIP};
        HashMap<Integer,Vector<Pair<String, String>>> caseIndexMap = new HashMap<>();

        try (PreparedStatement selectStatement = SqlHelper.prepareTableSelectProjectionStatement(connectionHandler.getConnection(),
                    TABLE_NAME,
                    projection, isPostgres(), getCurrentSchema())) {
            try (ResultSet resultSet = selectStatement.executeQuery()) {
                while (resultSet.next()) {
                    int caseRecordId = resultSet.getInt(resultSet.findColumn(COL_CASE_RECORD_ID));
                    String targetCase = resultSet.getString(resultSet.findColumn(COL_INDEX_TARGET));
                    String relationship = resultSet.getString(COL_INDEX_RELATIONSHIP);
                    Pair<String, String> index = new Pair<>(targetCase, relationship);

                    Vector<Pair<String, String>> indexList;
                    if (!caseIndexMap.containsKey(caseRecordId)) {
                        indexList = new Vector<>();
                    } else {
                        indexList = caseIndexMap.get(caseRecordId);
                    }
                    indexList.add(index);
                    caseIndexMap.put(caseRecordId, indexList);
                }
                return caseIndexMap;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a list of Case Record id's for cases which index a provided value.
     *
     * @param indexName   The name of the index
     * @param targetValue The case targeted by the index
     * @return An integer array of indexed case record ids
     */
    public LinkedHashSet<Integer> getCasesMatchingIndex(String indexName, String targetValue) {
        String[] args = new String[]{indexName, targetValue};

        if (log.isTraceEnabled()) {
            String query = String.format("SELECT %s FROM %s WHERE %s = ? AND %s = ?", COL_CASE_RECORD_ID, TABLE_NAME, COL_INDEX_NAME, COL_INDEX_TARGET);
            SqlHelper.explainSql(connectionHandler.getConnection(), query, args);
        }

        try (PreparedStatement selectStatement = SqlHelper.prepareTableSelectStatement(
                connectionHandler.getConnection(),
                    TABLE_NAME,
                    new String[]{COL_INDEX_NAME, COL_INDEX_TARGET},
                    args, isPostgres(), getCurrentSchema())) {
            try(ResultSet resultSet = selectStatement.executeQuery()) {
                LinkedHashSet<Integer> ret = new LinkedHashSet<>();
                SqlStorage.fillIdWindow(resultSet, COL_CASE_RECORD_ID, ret);
                return ret;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a list of Case Record id's for cases which index any of a set of provided values
     *
     * @param indexName      The name of the index
     * @param targetValueSet The set of cases targeted by the index
     * @return An integer array of indexed case record ids
     */
    public LinkedHashSet<Integer> getCasesMatchingValueSet(String indexName, String[] targetValueSet) {
        String[] args = new String[1 + targetValueSet.length];
        args[0] = indexName;

        System.arraycopy(targetValueSet, 0, args, 1, targetValueSet.length);
        String inSet = getArgumentBasedVariableSet(targetValueSet.length);

        String whereExpr = String.format("%s = ? AND %s IN %s", COL_INDEX_NAME, COL_INDEX_TARGET, inSet);

        try (PreparedStatement selectStatement = SqlHelper.prepareTableSelectStatement(
                connectionHandler.getConnection(),
                    TABLE_NAME,
                    whereExpr,
                    args, isPostgres(), getCurrentSchema())) {
            try (ResultSet resultSet = selectStatement.executeQuery()) {
                LinkedHashSet<Integer> ret = new LinkedHashSet<>();
                SqlStorage.fillIdWindow(resultSet, COL_CASE_RECORD_ID, ret);
                return ret;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int loadIntoIndexTable(HashMap<String, Vector<Integer>> indexCache, String indexName) {
        int resultsReturned = 0;
        String[] args = new String[]{indexName};

        try (PreparedStatement preparedStatement = SqlHelper.prepareTableSelectStatement(
                connectionHandler.getConnection(),
                TABLE_NAME,
                new String[]{COL_INDEX_NAME},
                args, isPostgres(), getCurrentSchema())) {

            try (ResultSet resultSet = preparedStatement.executeQuery()){
                while (resultSet.next()) {
                    resultsReturned++;
                    int id = resultSet.getInt(resultSet.findColumn(COL_CASE_RECORD_ID));
                    String target = resultSet.getString(resultSet.findColumn(COL_INDEX_TARGET));
                    String cacheID = indexName + "|" + target;
                    Vector<Integer> cache;
                    if (indexCache.containsKey(cacheID)) {
                        cache = indexCache.get(cacheID);
                    } else {
                        cache = new Vector<>();
                    }
                    cache.add(id);
                    indexCache.put(cacheID, cache);
                }
                return resultsReturned;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Provided an index name and a list of case row ID's, provides a list of the row ID's of the
     * cases which point to that ID
     *
     * @param cuedCases
     * @return
     */
    public DualTableSingleMatchModelQuerySet bulkReadIndexToCaseIdMatch(String indexName, Collection<Integer> cuedCases) {
        DualTableSingleMatchModelQuerySet set = new DualTableSingleMatchModelQuerySet();
        String caseIdIndex = TableBuilder.scrubName(Case.INDEX_CASE_ID);

        List<Pair<String, String[]>> whereParamList = TableBuilder.sqlList(cuedCases, "?");
        try {
            for (Pair<String, String[]> querySet : whereParamList) {

                String query = String.format(
                        "SELECT %s,%s " +
                                "FROM %s " +
                                "INNER JOIN %s " +
                                "ON %s = %s " +
                                "WHERE %s = '%s' " +
                                "AND " +
                                "%s IN %s",

                        COL_CASE_RECORD_ID, UserSqlSandbox.FORMPLAYER_CASE + "." + DatabaseHelper.ID_COL,
                        TABLE_NAME,
                        UserSqlSandbox.FORMPLAYER_CASE,
                        COL_INDEX_TARGET, caseIdIndex,
                        COL_INDEX_NAME, indexName,
                        COL_CASE_RECORD_ID, querySet.first);

                try (PreparedStatement preparedStatement =
                             connectionHandler.getConnection().prepareStatement(query)) {
                    int argIndex = 1;
                    for (String arg : querySet.second) {
                        preparedStatement.setString(argIndex, arg);
                        argIndex++;
                    }

                    if (log.isTraceEnabled()) {
                        SqlHelper.explainSql(connectionHandler.getConnection(), query, querySet.second);
                    }

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            int caseId = resultSet.getInt(resultSet.findColumn(COL_CASE_RECORD_ID));
                            int targetCase = resultSet.getInt(resultSet.findColumn(DatabaseHelper.ID_COL));
                            set.loadResult(caseId, targetCase);
                        }
                    }
                }
            }
            return set;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public static String getArgumentBasedVariableSet(int number) {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        for (int i = 0; i < number; i++) {
            sb.append('?');
            if (i < number - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private boolean isPostgres() {
        return connectionHandler instanceof PostgresDB;
    }

    private String getCurrentSchema() {
        if (isPostgres()) {
            return ((PostgresDB) connectionHandler).getCurrentSchema();
        } else {
            return null;
        }
    }
}
