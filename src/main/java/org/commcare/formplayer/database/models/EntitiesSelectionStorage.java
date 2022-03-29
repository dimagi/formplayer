package org.commcare.formplayer.database.models;

import com.google.common.base.Joiner;

import org.commcare.core.interfaces.EntitiesSelectionCache;
import org.commcare.formplayer.sandbox.SqlHelper;
import org.commcare.modern.database.DatabaseHelper;
import org.javarosa.core.services.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class EntitiesSelectionStorage implements EntitiesSelectionCache {

    private static final String TABLE_NAME = "entity_selection_cache";
    private static final String COL_KEY = "key";
    private static final String COL_VALUE = "value";
    private static final String ARRAY_SEPARATOR = ",";

    private final Connection handler;

    public EntitiesSelectionStorage(Connection connection) {
        this.handler = connection;

        try {
            execSQL(handler, getTableDefinition());
            // Need to commit in order to make these tables available
            if (!handler.getAutoCommit()) {
                handler.commit();
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
                    Logger.exception("Exception closing connection ", e);
                }
            }
        }
    }

    public static String getTableDefinition() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" +
                DatabaseHelper.ID_COL + " INTEGER PRIMARY KEY, " +
                COL_KEY + ", " +
                COL_VALUE +
                ")";
    }

    @Override
    public void cache(String cacheKey, String[] values) throws SQLException {
        HashMap<String, Object> contentValues = new HashMap<>();
        contentValues.put(COL_KEY, cacheKey);
        contentValues.put(COL_VALUE, serialize(values));
        SqlHelper.insertOrReplace(handler, TABLE_NAME, contentValues);
    }

    @Override
    public String[] read(String key) {
        try (PreparedStatement preparedStatement = SqlHelper.prepareTableSelectStatement(handler,
                TABLE_NAME,
                new String[]{COL_KEY},
                new String[]{key})) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String valuesStr = resultSet.getString(resultSet.findColumn(COL_VALUE));
                    return deserialize(valuesStr);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String serialize(String[] values) {
        return Joiner.on(ARRAY_SEPARATOR).join(values);
    }

    private String[] deserialize(String arrayStr) {
        return arrayStr.split(ARRAY_SEPARATOR);
    }

}
