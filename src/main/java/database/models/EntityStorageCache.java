package database.models;

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
    private static final String TAG = EntityStorageCache.class.getSimpleName();
    private static final String TABLE_NAME = "entity_cache";

    private static final String COL_CACHE_NAME = "cache_name";
    private static final String COL_ENTITY_KEY = "entity_key";
    private static final String COL_CACHE_KEY = "cache_key";
    private static final String COL_VALUE = "value";
    private static final String COL_TIMESTAMP = "timestamp";

    private final SQLiteConnectionPoolDataSource dataSource;
    private final String mCacheName;

    public EntityStorageCache(String cacheName, SQLiteConnectionPoolDataSource dataSource) {
        this.dataSource = dataSource;
        this.mCacheName = cacheName;
        try {
            execSQL(dataSource.getConnection(), getTableDefinition());
            EntityStorageCache.createIndexes(dataSource);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void execSQL(Connection connection, String query) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
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

    public static void createIndexes(SQLiteConnectionPoolDataSource dataSource) throws SQLException {
        execSQL(dataSource.getConnection(),
                DatabaseIndexingUtils.indexOnTableCommand("CACHE_TIMESTAMP", TABLE_NAME, COL_CACHE_NAME + ", " + COL_TIMESTAMP));
        execSQL(dataSource.getConnection(),
                DatabaseIndexingUtils.indexOnTableCommand("NAME_ENTITY_KEY", TABLE_NAME, COL_CACHE_NAME + ", " + COL_ENTITY_KEY + ", " + COL_CACHE_KEY));
    }

    //TODO: We should do some synchronization to make it the case that nothing can hold
    //an object for the same cache at once

    public void cache(String entityKey, String cacheKey, String value) throws SQLException {
        long timestamp = System.currentTimeMillis();
        //TODO: this should probably just be an ON CONFLICT REPLACE call
        Pair<String, String[]> wherePair =
                DatabaseHelper.createWhere(new String[]{COL_CACHE_NAME, COL_ENTITY_KEY, COL_CACHE_KEY},
                        new String[]{this.mCacheName, entityKey, cacheKey});
        SqlHelper.deleteFromTableWhere(dataSource.getConnection(), TABLE_NAME, wherePair.first, wherePair.second);
        //We need to clear this cache value if it exists first.
        HashMap<String, String> contentValues = new HashMap<>();
        contentValues.put(COL_CACHE_NAME, mCacheName);
        contentValues.put(COL_ENTITY_KEY, entityKey);
        contentValues.put(COL_CACHE_KEY, cacheKey);
        contentValues.put(COL_VALUE, value);
        contentValues.put(COL_TIMESTAMP, String.valueOf(timestamp));
        SqlHelper.basicInsert(dataSource.getConnection(), TABLE_NAME, contentValues);
    }

    public String retrieveCacheValue(String entityKey, String cacheKey) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {

            connection = dataSource.getConnection();
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
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Removes cache records associated with the provided ID
     */
    public void invalidateCache(String recordId) {
        Pair<String, String[]> wherePair =
                DatabaseHelper.createWhere(new String[]{COL_CACHE_NAME, COL_ENTITY_KEY},
                        new String[]{this.mCacheName, recordId});
        try {
            SqlHelper.deleteFromTableWhere(dataSource.getConnection(), TABLE_NAME, wherePair.first, wherePair.second);
        } catch (SQLException e) {
            throw new RuntimeException(e);
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
