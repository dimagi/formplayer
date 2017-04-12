package database.models;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.database.TableBuilder;
import sandbox.SqlHelper;
import org.commcare.modern.database.DatabaseHelper;
import org.commcare.modern.database.DatabaseIndexingUtils;
import org.commcare.modern.util.Pair;
import sandbox.UserSqlSandbox;
import services.ConnectionHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author wspride
 */
public class EntityStorageCache {

    private static final String TABLE_NAME = "entitycache";

    private static final String COL_CACHE_NAME = "cache_name";
    private static final String COL_ENTITY_KEY = "entity_key";
    private static final String COL_CACHE_KEY = "cache_key";
    private static final String COL_VALUE = "value";
    private static final String COL_TIMESTAMP = "timestamp";

    private final ConnectionHandler handler;
    private final String mCacheName;

    private static final Log log = LogFactory.getLog(EntityStorageCache.class);

    public EntityStorageCache(String cacheName, ConnectionHandler handler) {
        this.mCacheName = cacheName;
        this.handler = handler;
        try {
            execSQL(handler.getConnection(), getTableDefinition());
            EntityStorageCache.createIndexes(handler.getConnection());
            // Need to commit in order to make these tables available
            if (!handler.getConnection().getAutoCommit()) {
                handler.getConnection().commit();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
                    log.debug("Exception closing connection ", e);
                }
            }
        }
    }

    public static String getTableDefinition() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" +
                DatabaseHelper.ID_COL + " INTEGER PRIMARY KEY, " +
                COL_CACHE_NAME + ", " +
                COL_ENTITY_KEY + ", " +
                COL_CACHE_KEY + ", " +
                COL_VALUE + ", " +
                COL_TIMESTAMP +
                ")";
    }

    public static void createIndexes(Connection connection) throws SQLException {
        execSQL(connection,
                DatabaseIndexingUtils.indexOnTableCommand("CACHE_TIMESTAMP", TABLE_NAME, COL_CACHE_NAME + ", " + COL_TIMESTAMP));
        execSQL(connection,
                DatabaseIndexingUtils.indexOnTableCommand("NAME_ENTITY_KEY", TABLE_NAME, COL_CACHE_NAME + ", " + COL_ENTITY_KEY + ", " + COL_CACHE_KEY));
    }

    // Currently unused
    public void cache(String entityKey, String cacheKey, String value) throws SQLException {
        long timestamp = System.currentTimeMillis();
        //TODO: this should probably just be an ON CONFLICT REPLACE call
        Pair<String, String[]> wherePair =
                DatabaseHelper.createWhere(new String[]{COL_CACHE_NAME, COL_ENTITY_KEY, COL_CACHE_KEY},
                        new String[]{this.mCacheName, entityKey, cacheKey});
        SqlHelper.deleteFromTableWhere(handler.getConnection(), TABLE_NAME, wherePair.first, wherePair.second);
        //We need to clear this cache value if it exists first.
        HashMap<String, String> contentValues = new HashMap<>();
        contentValues.put(COL_CACHE_NAME, mCacheName);
        contentValues.put(COL_ENTITY_KEY, entityKey);
        contentValues.put(COL_CACHE_KEY, cacheKey);
        contentValues.put(COL_VALUE, value);
        contentValues.put(COL_TIMESTAMP, String.valueOf(timestamp));
        SqlHelper.basicInsert(handler.getConnection(), TABLE_NAME, contentValues);
    }

    // Currently unused
    public String retrieveCacheValue(String entityKey, String cacheKey) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = SqlHelper.prepareTableSelectStatement(connection,
                    TABLE_NAME,
                    new String[]{COL_CACHE_NAME, COL_ENTITY_KEY, COL_CACHE_KEY},
                    new String[]{mCacheName, entityKey, cacheKey});
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString(resultSet.findColumn(COL_VALUE));
            } else {
                return null;
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
     * Removes cache records associated with the provided ID
     */
    public void invalidateCache(String recordId) {
        Pair<String, String[]> wherePair =
                DatabaseHelper.createWhere(new String[]{COL_CACHE_NAME, COL_ENTITY_KEY},
                        new String[]{this.mCacheName, recordId});
        SqlHelper.deleteFromTableWhere(handler.getConnection(), TABLE_NAME, wherePair.first, wherePair.second);
    }

    /**
     * Removes cache records associated with the provided IDs
     */
    public void invalidateCaches(Collection<Integer> recordIds) {
        if (recordIds.size() == 0) {
            return;
        }
        List<Pair<String, String[]>> whereParamList = TableBuilder.sqlList(recordIds);
        for(Pair<String, String[]> querySet : whereParamList) {
            String[] updated = new String[querySet.second.length + 1];
            System.arraycopy(querySet.second, 0, updated, 1, querySet.second.length);
            updated[0] = this.mCacheName;
            SqlHelper.deleteFromTableWhere(handler.getConnection(),
                    TABLE_NAME,
                    MessageFormat.format("{0} = ? AND {1} IN {2}", COL_CACHE_NAME, COL_ENTITY_KEY, querySet.first),
                    updated);
        }
    }

    public static int getSortFieldIdFromCacheKey(String detailId, String cacheKey) {
        String intId = cacheKey.substring(detailId.length() + 1);
        try {
            return Integer.parseInt(intId);
        } catch (NumberFormatException nfe) {
            //TODO: Kill this cache key if this didn't work
            return -1;
        }
    }
}
