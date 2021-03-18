package org.commcare.formplayer.sandbox;

import org.commcare.formplayer.exceptions.SQLiteRuntimeException;

import org.commcare.formplayer.postgresutil.PostgresDB;
import org.commcare.formplayer.postgresutil.PostgresDatabaseHelper;
import org.commcare.formplayer.postgresutil.PostgresSqlHelper;
import org.commcare.formplayer.sqliteutil.SqliteSqlHelper;
import org.commcare.modern.util.Pair;
import org.commcare.modern.database.*;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.Persistable;

import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Set of Sql utility methods for clients running modern, non-Android Java (where prepared
 * statements in the place of cursors)
 * <p/>
 * All methods that return a ResultSet expect a PreparedStatement as an argument and the caller
 * is responsible for closing this statement when its finished with it.
 * <p/>
 * Created by wpride1 on 8/11/15.
 */
public class SqlHelper {

    public static void explainSql(Connection c, String sql, String[] args) {
        try (PreparedStatement preparedStatement = c.prepareStatement("EXPLAIN QUERY PLAN " + sql)){
            for (int i = 1; i <= args.length; i++) {
                preparedStatement.setString(i, args[i - 1]);
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                dumpResultSet(resultSet);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the contents of a Cursor to System.out. The position is restored
     * after printing.
     *
     * @param resultSet the ResultSet to print
     */
    public static void dumpResultSet(ResultSet resultSet) throws SQLException {
        dumpResultSet(resultSet, System.out);
    }

    /**
     * Prints the contents of a Cursor to a PrintSteam. The position is restored
     * after printing.
     *
     * @param resultSet the ResultSet to print
     * @param stream    the stream to print to
     */
    public static void dumpResultSet(ResultSet resultSet, PrintStream stream) throws SQLException {
        stream.println(">>>>> Dumping cursor " + resultSet);
        ResultSetMetaData metaData = resultSet.getMetaData();
        while (resultSet.next()) {
            dumpResultSetRow(metaData, resultSet, stream);
        }
        stream.println("<<<<<");
    }

    private static void dumpResultSetRow(ResultSetMetaData metaData, ResultSet resultSet, PrintStream stream) throws SQLException {
        stream.println("" + resultSet.getRow() + " {");
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (i > 1) stream.print(",  ");
            String columnValue = resultSet.getString(i);
            stream.println("   " + metaData.getColumnName(i) + "=" + columnValue);
        }
        stream.println("}");
    }

    public static void dropTable(Connection c, String storageKey) {
        String sqlStatement = "DROP TABLE IF EXISTS " + storageKey;
        try (PreparedStatement preparedStatement = c.prepareStatement(sqlStatement)) {
            preparedStatement.execute();
        } catch (SQLException e) {
            Logger.log("E", "Could not drop table: " + e.getMessage());
        }
    }

    public static void createTable(Connection c, String storageKey, Persistable p, boolean isPostgres, String currentSchema) {
        String sqlStatement;
        if (isPostgres) {
            sqlStatement = PostgresDatabaseHelper.getTableCreateString(storageKey, p, currentSchema);
        } else {
            sqlStatement = DatabaseHelper.getTableCreateString(storageKey, p);
        }
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = c.prepareStatement(sqlStatement);
            preparedStatement.execute();
            preparedStatement.close();

            if (storageKey.equals(UserSqlSandbox.FORMPLAYER_CASE)) {
                preparedStatement = c.prepareStatement(DatabaseIndexingUtils.indexOnTableCommand("case_id_index", UserSqlSandbox.FORMPLAYER_CASE, "case_id"));
                preparedStatement.execute();
                preparedStatement.close();

                preparedStatement = c.prepareStatement(DatabaseIndexingUtils.indexOnTableCommand("case_type_index", UserSqlSandbox.FORMPLAYER_CASE, "case_type"));
                preparedStatement.execute();
                preparedStatement.close();

                preparedStatement = c.prepareStatement(DatabaseIndexingUtils.indexOnTableCommand("case_status_index", UserSqlSandbox.FORMPLAYER_CASE, "case_status"));
                preparedStatement.execute();
                preparedStatement.close();

                preparedStatement = c.prepareStatement(DatabaseIndexingUtils.indexOnTableCommand("case_status_open_index", UserSqlSandbox.FORMPLAYER_CASE, "case_type,case_status"));
                preparedStatement.execute();
                preparedStatement.close();

            }
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get a prepared statement to select matching rows by the internal ID column
     *
     * Note: Caller is responsible for ensuring the prepared statement is closed
     *
     */
    public static PreparedStatement prepareIdSelectStatement(Connection c, String storageKey, int id) {
        try {
            PreparedStatement preparedStatement =
                    c.prepareStatement("SELECT * FROM " + storageKey + " WHERE "
                            + DatabaseHelper.ID_COL + " = ?;");
            preparedStatement.setInt(1, id);
            return preparedStatement;
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    /**
     * Get a prepared statement to select matching rows by multiple storage keys
     *
     * Note: Caller is responsible for ensuring the prepared statement is closed
     *
     */
    public static PreparedStatement prepareTableSelectProjectionStatement(Connection c,
                                                                          String storageKey,
                                                                          String[] projections) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < projections.length; i++) {
            builder.append(projections[i]);
            if (i + 1 < projections.length) {
                builder.append(", ");
            }
        }
        String queryString = "SELECT " + builder.toString() + " FROM " + storageKey + ";";
        try {
            return c.prepareStatement(queryString);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a prepared statement to select matching appropriate metadata fields from
     * an individual record
     *
     * Note: Caller is responsible for ensuring the prepared statement is closed
     *
     */
    public static PreparedStatement prepareTableSelectProjectionStatement(Connection c,
                                                                          String storageKey,
                                                                          String recordId,
                                                                          String[] projections) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < projections.length; i++) {
            builder.append(projections[i]);
            if (i + 1 < projections.length) {
                builder.append(", ");
            }
        }
        String queryString = "SELECT " + builder.toString() +
                " FROM " + storageKey +
                " WHERE " + DatabaseHelper.ID_COL + " = ?;";
        try {
            PreparedStatement preparedStatement = c.prepareStatement(queryString);
            preparedStatement.setString(1, recordId);
            return preparedStatement;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @throws IllegalArgumentException when one or more of the fields we're selecting on
     *                                  is not a valid key to select on for this object
     */
    public static PreparedStatement prepareTableSelectStatementProjection(Connection c,
                                                                          String storageKey,
                                                                          String where,
                                                                          String values[],
                                                                          String[] projections) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < projections.length; i++) {
            builder.append(projections[i]);
            if (i + 1 < projections.length) {
                builder.append(", ");
            }
        }
        try {
            String queryString =
                    "SELECT " + builder.toString() + " FROM " + storageKey + " WHERE " + where + ";";
            PreparedStatement preparedStatement = c.prepareStatement(queryString);
            for (int i = 0; i < values.length; i++) {
                preparedStatement.setString(i + 1, values[i]);
            }
            return preparedStatement;
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    /**
     * @throws IllegalArgumentException when one or more of the fields we're selecting on
     *                                  is not a valid key to select on for this object
     */
    public static PreparedStatement prepareTableSelectStatement(Connection c,
                                                                String storageKey,
                                                                String[] fields,
                                                                Object[] values) {
        Pair<String, String[]> pair = DatabaseHelper.createWhere(fields, values, null);
        try {
            String queryString =
                    "SELECT * FROM " + storageKey + " WHERE " + pair.first + ";";
            PreparedStatement preparedStatement = c.prepareStatement(queryString);
            for (int i = 0; i < pair.second.length; i++) {
                preparedStatement.setString(i + 1, pair.second[i]);
            }
            return preparedStatement;
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    /**
     * @throws IllegalArgumentException when one or more of the fields we're selecting on
     *                                  is not a valid key to select on for this object
     */
    public static PreparedStatement prepareTableSelectStatement(Connection c,
                                                                String storageKey,
                                                                String where,
                                                                String values[]) {
        try {
            String queryString =
                    "SELECT * FROM " + storageKey + " WHERE " + where + ";";
            PreparedStatement preparedStatement = c.prepareStatement(queryString);
            for (int i = 0; i < values.length; i++) {
                preparedStatement.setString(i + 1, values[i]);
            }
            return preparedStatement;
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    private static void performInsert(Connection c,
                                      Pair<List<Object>, String> valsAndInsertStatement, boolean isPostgres) {
        try (PreparedStatement preparedStatement = c.prepareStatement(valsAndInsertStatement.second)) {
            int i = 1;

            for (Object val : valsAndInsertStatement.first) {
                setArgumentToSqlStatement(preparedStatement, val, i++, isPostgres);
            }
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    public static void basicInsert(Connection c,
                                   String storageKey,
                                   Map<String, Object> contentVals,
                                   boolean isPostgres) {
        Pair<List<Object>, String> valsAndInsertStatement =
                buildInsertStatement(storageKey, contentVals);
        performInsert(c, valsAndInsertStatement, isPostgres);
    }

    public static void insertOrReplace(Connection c,
                                       String storageKey,
                                       Map<String, Object> contentValues,
                                       boolean isPostgres) {
        Pair<List<Object>, String> valsAndInsertStatement =
                buildInsertOrReplaceStatement(storageKey, contentValues);
        performInsert(c, valsAndInsertStatement, isPostgres);
    }

    private static Pair<List<Object>, String> buildInsertStatement(String storageKey,
                                                                   Map<String, Object> contentVals,
                                                                   String insertStatement) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(insertStatement).append(storageKey).append(" (");
        List<Object> values = new ArrayList<>();
        String prefix = "";
        for (String key : contentVals.keySet()) {
            stringBuilder.append(prefix);
            prefix = ",";
            stringBuilder.append(key);
            values.add(contentVals.get(key));
        }
        stringBuilder.append(") VALUES (");
        prefix = "";
        for (int i = 0; i < values.size(); i++) {
            stringBuilder.append(prefix);
            prefix = ",";
            stringBuilder.append("?");
        }
        stringBuilder.append(");");
        return Pair.create(values, stringBuilder.toString());
    }

    private static Pair<List<Object>, String> buildInsertStatement(String storageKey,
                                                                   Map<String, Object> contentVals) {
        return buildInsertStatement(storageKey, contentVals, "INSERT INTO ");
    }

    private static Pair<List<Object>, String> buildInsertOrReplaceStatement(String storageKey,
                                                                            Map<String, Object> contentVals) {
        return buildInsertStatement(storageKey, contentVals, "INSERT OR REPLACE INTO ");
    }

    public static int insertToTable(Connection c, String storageKey, Persistable p, boolean isPostgres, String currentSchema) {
        if (isPostgres) {
            return PostgresSqlHelper.insertToTable(c, storageKey, p, currentSchema);
        } else {
            return SqliteSqlHelper.insertToTable(c, storageKey, p);
        }
    }

    private static void setArgumentToSqlStatement(PreparedStatement preparedStatement, Object arg, int index, boolean isPostgres) throws SQLException {
        if (isPostgres) {
            PostgresSqlHelper.setArgumentToSqlStatement(preparedStatement, arg, index);
        } else {
            SqliteSqlHelper.setArgumentToSqlStatement(preparedStatement, arg, index);
        }
    }

    /**
     * Update SQLite DB with Persistable p
     *
     * @param c          Database Connection
     * @param storageKey name of table
     * @param p          persistable to be updated
     */
    public static void updateId(Connection c, String storageKey, Persistable p, boolean isPostgres) {
        HashMap<String, Object> map = DatabaseHelper.getMetaFieldsAndValues(p);

        String[] fieldNames = map.keySet().toArray(new String[map.keySet().size()]);
        Object[] values = map.values().toArray(new Object[map.values().size()]);

        Pair<String, String[]> where = org.commcare.modern.database.DatabaseHelper.createWhere(fieldNames, values, p);

        String query = "UPDATE " + storageKey + " SET " + DatabaseHelper.DATA_COL + " = ? WHERE " + where.first + ";";

        try (PreparedStatement preparedStatement = c.prepareStatement(query)){
            setPreparedStatementArgs(preparedStatement, p, where.second, isPostgres);
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
    public static void updateToTable(Connection connection, String tableName, Persistable persistable, int id, boolean isPostgres, String currentSchema) {
        if (isPostgres) {
            PostgresSqlHelper.updateToTable(connection, tableName, persistable, id, currentSchema);
        } else {
            SqliteSqlHelper.updateToTable(connection, tableName, persistable, id);
        }
    }

    /**
     * @param preparedStatement the PreparedStatement to populate with arguments
     * @param persistable       the Persistable object being stored
     * @param values            the ordered values to use in the PreparedStatement (corresponding to the
     *                          '?' in the query string)
     * @return the index of the next '?' NOT populated by this helper
     */
    public static int setPreparedStatementArgs(PreparedStatement preparedStatement,
                                               Persistable persistable,
                                               Object[] values,
                                               boolean isPostgres) throws SQLException {
        if (isPostgres) {
            return PostgresSqlHelper.setPreparedStatementArgs(preparedStatement, persistable, values);
        } else {
            return SqliteSqlHelper.setPreparedStatementArgs(preparedStatement, persistable, values);
        }
    }

    public static void deleteFromTableWhere(Connection connection, String tableName, String whereClause, String arg) {
        String query = "DELETE FROM " + tableName + " WHERE " + whereClause + ";";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(1, arg);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    public static void deleteFromTableWhere(Connection connection, String tableName, String whereClause, String[] args) {
        String query = "DELETE FROM " + tableName + " WHERE " + whereClause + ";";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            for (int i = 1; i <= args.length; i++) {
                preparedStatement.setString(i, args[i - 1]);
            }
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    /**
     * Update entry under id with persistable p
     *
     * @param connection Database Connection
     * @param tableName  name of table
     * @param id         sql record to update
     */
    public static void deleteIdFromTable(Connection connection, String tableName, int id) {
        String query = "DELETE FROM " + tableName + " WHERE " + DatabaseHelper.ID_COL + " = ?;";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, id);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    /**
     * Update entry under id with persistable p
     *
     * @param connection Database Connection
     * @param tableName  name of table
     */
    public static void deleteAllFromTable(Connection connection, String tableName) {

        if (!isTableExist(connection, tableName)) {
            return;
        }

        String query = "DELETE FROM " + tableName;
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        }
    }

    public static boolean isTableExist(Connection connection, String tableName) {
        ResultSet resultSet = null;
        try {
            DatabaseMetaData md = connection.getMetaData();
            resultSet = md.getTables(null, null, tableName, null);
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            throw new SQLiteRuntimeException(e);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return false;
    }
}
