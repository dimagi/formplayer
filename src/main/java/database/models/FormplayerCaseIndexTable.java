package database.models;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.cases.model.Case;
import org.commcare.cases.model.CaseIndex;
import org.commcare.cases.query.queryset.DualTableSingleMatchModelQuerySet;
import org.commcare.modern.database.DatabaseHelper;
import org.commcare.modern.database.DatabaseIndexingUtils;
import org.commcare.modern.database.TableBuilder;
import org.commcare.modern.engine.cases.CaseIndexTable;
import org.commcare.modern.util.Pair;
import sandbox.SqlHelper;
import sandbox.SqliteIndexedStorageUtility;
import sandbox.UserSqlSandbox;
import services.ConnectionHandler;

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
            HashMap<String, String> contentValues = new HashMap<>();
            contentValues.put(COL_CASE_RECORD_ID, "" + c.getID());
            contentValues.put(COL_INDEX_NAME, ci.getName());
            contentValues.put(COL_INDEX_TYPE, ci.getTargetType());
            contentValues.put(COL_INDEX_TARGET, ci.getTarget());
            SqlHelper.basicInsert(connectionHandler.getConnection(), TABLE_NAME, contentValues);
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
                recordIdString);
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
                    whereParams.second);
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
        PreparedStatement selectStatement = null;
        try {

            if (SqlHelper.SQL_DEBUG) {
                String query = String.format("SELECT %s FROM %s WHERE %s = ? AND %s = ?", COL_CASE_RECORD_ID, TABLE_NAME, COL_INDEX_NAME, COL_INDEX_TARGET);
                SqlHelper.explainSql(connectionHandler.getConnection(), query, args);
            }

            selectStatement = SqlHelper.prepareTableSelectStatement(connectionHandler.getConnection(),
                    TABLE_NAME,
                    new String[]{COL_INDEX_NAME, COL_INDEX_TARGET},
                    args);
            ResultSet resultSet = selectStatement.executeQuery();
            LinkedHashSet<Integer> ret = new LinkedHashSet<>();
            SqliteIndexedStorageUtility.fillIdWindow(resultSet, COL_CASE_RECORD_ID, ret);
            return ret;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (selectStatement != null) {
                try {
                    selectStatement.close();
                } catch (SQLException e) {
                    log.debug("Exception closing connection ", e);
                }
            }
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

        PreparedStatement selectStatement = null;
        try {
            selectStatement = SqlHelper.prepareTableSelectStatement(connectionHandler.getConnection(),
                    TABLE_NAME,
                    whereExpr,
                    args);
            ResultSet resultSet = selectStatement.executeQuery();
            LinkedHashSet<Integer> ret = new LinkedHashSet<>();
            SqliteIndexedStorageUtility.fillIdWindow(resultSet, COL_CASE_RECORD_ID, ret);
            return ret;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (selectStatement != null) {
                try {
                    selectStatement.close();
                } catch (SQLException e) {
                    log.debug("Exception prepared statement connection ", e);
                }
            }
        }
    }

    public int loadIntoIndexTable(HashMap<String, Vector<Integer>> indexCache, String indexName) {
        int resultsReturned = 0;
        String[] args = new String[]{indexName};

        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = SqlHelper.prepareTableSelectStatement(connectionHandler.getConnection(),
                    TABLE_NAME,
                    new String[]{COL_INDEX_NAME},
                    args);
            ResultSet resultSet = preparedStatement.executeQuery();
            try {
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
            } finally {
                if (resultSet != null) {
                    resultSet.close();
                }
            }
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
        PreparedStatement preparedStatement = null;
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

                preparedStatement = connectionHandler.getConnection().prepareStatement(query);
                int argIndex = 1;
                for (String arg: querySet.second) {
                    preparedStatement.setString(argIndex, arg);
                    argIndex++;
                }

                if (SqlHelper.SQL_DEBUG) {
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
            return set;
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

}
