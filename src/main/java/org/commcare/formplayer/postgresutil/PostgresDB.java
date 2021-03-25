package org.commcare.formplayer.postgresutil;

import org.commcare.formplayer.exceptions.SQLiteRuntimeException;
import org.commcare.formplayer.services.ConnectionHandler;
import org.commcare.formplayer.sqlitedb.DBPath;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author $|-|!˅@M
 */
public class PostgresDB implements ConnectionHandler {
    private DBPath dbPath;
    private Connection connection;
    private PostgresProperties properties;

    public PostgresDB(DBPath dbPath, PostgresProperties properties) {
        this.dbPath = dbPath;
        this.properties = properties;
        if (dbPath != null) {
            // Create the schema before doing anything.
            runQuery("CREATE SCHEMA IF NOT EXISTS " + getCurrentSchema() + ";");
        }
    }

    public String getCurrentSchema() {
        return dbPath.getDatabasePath();
    }

    private Connection getNewConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            Properties props = new Properties();
            props.setProperty("user", properties.getUsername());
            props.setProperty("password", properties.getPassword());
            return DriverManager.getConnection(properties.getUrl(), props);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = getNewConnection();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connection = null;
    }

    public void deleteDatabase() {
        if (dbPath != null) {
            runQuery("DROP SCHEMA IF EXISTS " + getCurrentSchema() + " CASCADE;");
        }
    }

    public boolean tableExists(String name) {
        String query = "SELECT to_regclass('" + getCurrentSchema() + "." + name + "');";
        Connection connection = getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getObject(resultSet.findColumn("to_regclass")) != null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void runQuery(String query) {
        Connection connection = getConnection();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.execute();
            preparedStatement.close();
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
}
