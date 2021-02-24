package org.commcare.formplayer.postgresutil;

import org.apache.commons.lang3.ArrayUtils;
import org.commcare.formplayer.exceptions.SQLiteRuntimeException;
import org.commcare.formplayer.services.ConnectionHandler;
import org.commcare.modern.database.DatabaseHelper;
import org.commcare.modern.util.Pair;
import org.javarosa.core.services.storage.Persistable;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.commcare.modern.database.TableBuilder.scrubName;

/**
 * @author $|-|!˅@M
 */
public class PostgresSqlHelper {

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
        int i = 1;
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
     * Performs upsert, i.e. we'll first try to insert the new value and if it already exists, we'll update it.
     * https://www.postgresql.org/docs/devel/sql-insert.html#SQL-ON-CONFLICT
     *
     * @param connection    Database Connection
     * @param tableName     name of table
     * @param persistable   persistable to update with
     * @param id            sql record to update
     * @param currentSchema Database schema
     */
    public static void upsertToTable(Connection connection, String tableName, Persistable persistable, int id, String currentSchema) {
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ")
                .append(currentSchema)
                .append(".")
                .append(tableName)
                .append(" (")
                .append(DatabaseHelper.ID_COL);

        HashMap<String, Object> map = DatabaseHelper.getMetaFieldsAndValues(persistable);
        String[] fieldNames = map.keySet().toArray(new String[map.keySet().size()]);
        Object[] values = map.values().toArray(new Object[map.values().size()]);
        Object[] params = ArrayUtils.addAll(values, values);
        for (String field: fieldNames) {
            query.append(", ")
                    .append(field);
        }

        query.append(") VALUES (").append(id);

        for (Object obj: values) {
            query.append(", ?");
        }
        query.append(") ON CONFLICT (")
                .append(DatabaseHelper.ID_COL)
                .append(") Do UPDATE SET ");
        for (String fieldName : fieldNames) {
            query.append(fieldName).append(" = ?").append(", ");
        }
        query.deleteCharAt(query.lastIndexOf(","));
        query.append(";");

        try (PreparedStatement preparedStatement = connection.prepareStatement(query.toString())){
            setPreparedStatementArgs(preparedStatement, persistable, params);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }


    /**
     * Update entry under id with persistable p
     *
     * @param connection  Database Connection
     * @param tableName   name of table
     * @param persistable persistable to update with
     * @param id          sql record to update
     */
    public static void updateToTable(Connection connection, String tableName, Persistable persistable, int id, String currentSchema) {
        upsertToTable(connection, tableName, persistable, id, currentSchema);
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
            preparedStatement.setString(index, "");
        }
    }


    public static int insertToTable(Connection c, String storageKey, Persistable p, String currentSchema) {
        Pair<String, List<Object>> mPair = PostgresDatabaseHelper.getTableInsertData(storageKey, p, currentSchema);

        try (PreparedStatement preparedStatement = c.prepareStatement(mPair.first, Statement.RETURN_GENERATED_KEYS)) {
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
