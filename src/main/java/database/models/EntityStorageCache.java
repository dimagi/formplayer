package database.models;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.persistence.SqlHelper;
import org.commcare.modern.database.DatabaseHelper;
import org.commcare.modern.database.DatabaseIndexingUtils;
import org.commcare.modern.util.Pair;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * @author wspride
 */
public class EntityStorageCache {

    private static final String TABLE_NAME = "entity_cache";

    private static final String COL_CACHE_NAME = "cache_name";
    private static final String COL_ENTITY_KEY = "entity_key";
    private static final String COL_CACHE_KEY = "cache_key";
    private static final String COL_VALUE = "value";
    private static final String COL_TIMESTAMP = "timestamp";

    private final Connection connection;
    private final String mCacheName;

    private static final Log log = LogFactory.getLog(EntityStorageCache.class);

    public EntityStorageCache(String cacheName, Connection connection) {
        this.mCacheName = cacheName;
        this.connection = connection;
        try {
            execSQL(connection, getTableDefinition());
            EntityStorageCache.createIndexes(connection);
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
        return "CREATE TABLE " + TABLE_NAME + "(" +
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
        SqlHelper.deleteFromTableWhere(connection, TABLE_NAME, wherePair.first, wherePair.second);
        //We need to clear this cache value if it exists first.
        HashMap<String, String> contentValues = new HashMap<>();
        contentValues.put(COL_CACHE_NAME, mCacheName);
        contentValues.put(COL_ENTITY_KEY, entityKey);
        contentValues.put(COL_CACHE_KEY, cacheKey);
        contentValues.put(COL_VALUE, value);
        contentValues.put(COL_TIMESTAMP, String.valueOf(timestamp));
        SqlHelper.basicInsert(connection, TABLE_NAME, contentValues);
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
        SqlHelper.deleteFromTableWhere(connection, TABLE_NAME, wherePair.first, wherePair.second);
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
