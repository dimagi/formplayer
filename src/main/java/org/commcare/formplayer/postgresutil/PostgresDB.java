package org.commcare.formplayer.postgresutil;

import org.commcare.formplayer.exceptions.SQLiteRuntimeException;
import org.commcare.formplayer.services.ConnectionHandler;
import org.commcare.formplayer.sqlitedb.DBPath;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author $|-|!˅@M
 */
public class PostgresDB implements ConnectionHandler {
    private DBPath dbPath;
    private Connection connection;

    public PostgresDB(DBPath dbPath) {
        this.dbPath = dbPath;
        if (dbPath != null) {
            // Create the schema before doing anything.
            Connection connection = getConnection();
            String schemaCreationSt = "CREATE SCHEMA IF NOT EXISTS " + getCurrentSchema() + ";";
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = connection.prepareStatement(schemaCreationSt);
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

    public String getCurrentSchema() {
        return dbPath.getDatabasePath();
    }

    private Connection getNewConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            Properties props = new Properties();
            props.setProperty("user", "commcarehq");
            props.setProperty("password", "commcarehq");
            props.setProperty("currentSchema", getCurrentSchema());
            return DriverManager.getConnection("jdbc:postgresql://localhost:5432/formplayer", props);
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
}
