package org.commcare.formplayer.sqliteutil;

import org.commcare.formplayer.exceptions.SQLiteRuntimeException;
import org.commcare.modern.database.DatabaseHelper;
import org.commcare.modern.util.Pair;
import org.javarosa.core.services.storage.Persistable;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

/**
 * @author $|-|!˅@M
 */
public class SqliteSqlHelper {

    /**
     * @param preparedStatement the PreparedStatement to populate with arguments
     * @param persistable       the Persistable object being stored
     * @param values            the ordered values to use in the PreparedStatement (corresponding to the
     *                          '?' in the query string)
     * @return the index of the next '?' NOT populated by this helper
     */
    public static int setPreparedStatementArgs(PreparedStatement preparedStatement,
                                               Persistable persistable,
                                               Object[] values) throws SQLException {
        byte[] blob = org.commcare.modern.database.TableBuilder.toBlob(persistable);
        preparedStatement.setBinaryStream(1, new ByteArrayInputStream(blob), blob.length);
        // offset to 2 since 1) SQLite is 1 indexed and 2) we set the first arg above
        int i = 2;
        for (Object obj : values) {
            if (obj instanceof String) {
                preparedStatement.setString(i, (String)obj);
            } else if (obj instanceof Blob) {
                preparedStatement.setBlob(i, (Blob)obj);
            } else if (obj instanceof Integer) {
                preparedStatement.setInt(i, (Integer)obj);
            } else if (obj instanceof Long) {
                preparedStatement.setLong(i, (Long)obj);
            } else if (obj instanceof byte[]) {
                preparedStatement.setBinaryStream(i, new ByteArrayInputStream((byte[])obj), ((byte[])obj).length);
            } else if (obj == null) {
                preparedStatement.setNull(i, 0);
            }
            i++;
        }
        return i;
    }

    /**
     * Update entry under id with persistable p
     *
     * @param connection  Database Connection
     * @param tableName   name of table
     * @param persistable persistable to update with
     * @param id          sql record to update
     */
    public static void updateToTable(Connection connection, String tableName, Persistable persistable, int id) {
        String queryStart = "UPDATE " + tableName + " SET " + DatabaseHelper.DATA_COL + " = ? ";
        String queryEnd = " WHERE " + DatabaseHelper.ID_COL + " = ?;";

        HashMap<String, Object> map = DatabaseHelper.getMetaFieldsAndValues(persistable);
        String[] fieldNames = map.keySet().toArray(new String[map.keySet().size()]);
        Object[] values = map.values().toArray(new Object[map.values().size()]);

        StringBuilder stringBuilder = new StringBuilder(queryStart);
        for (String fieldName : fieldNames) {
            stringBuilder.append(", ").append(fieldName).append(" = ?");
        }

        String query = stringBuilder.append(queryEnd).toString();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            int lastArgIndex = setPreparedStatementArgs(preparedStatement, persistable, values);
            preparedStatement.setInt(lastArgIndex, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    public static void setArgumentToSqlStatement(PreparedStatement preparedStatement, Object arg, int index) throws SQLException {
        if (arg instanceof String) {
            preparedStatement.setString(index, (String)arg);
        } else if (arg instanceof Blob) {
            preparedStatement.setBlob(index, (Blob)arg);
        } else if (arg instanceof Integer) {
            preparedStatement.setInt(index, (Integer)arg);
        } else if (arg instanceof Long) {
            preparedStatement.setLong(index, (Long)arg);
        } else if (arg instanceof byte[]) {
            preparedStatement.setBinaryStream(index, new ByteArrayInputStream((byte[])arg), ((byte[])arg).length);
        } else if(arg == null) {
            preparedStatement.setNull(index, 0);
        }
    }

    public static int insertToTable(Connection c, String storageKey, Persistable p) {
        Pair<String, List<Object>> mPair = DatabaseHelper.getTableInsertData(storageKey, p);

        try (PreparedStatement preparedStatement = c.prepareStatement(mPair.first)) {
            for (int i = 0; i < mPair.second.size(); i++) {
                Object obj = mPair.second.get(i);
                setArgumentToSqlStatement(preparedStatement, obj, i+1);
            }
            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                try {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        p.setID(id);
                        return id;
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                } finally {
                    if (generatedKeys != null) {
                        generatedKeys.close();
                    }
                }
            }
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }
}
