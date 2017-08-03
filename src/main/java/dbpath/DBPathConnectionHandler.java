package dbpath;

import org.apache.commons.logging.Log;
import org.sqlite.SQLiteConnection;
import sandbox.SqlSandboxUtils;
import services.ConnectionHandler;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class DBPathConnectionHandler implements ConnectionHandler {
    private DBPath dbPath;
    private Log log;
    private Connection connection;

    public DBPathConnectionHandler(DBPath dbPath, Log log) {
        this.dbPath = dbPath;
        this.log = log;
    }

    private Connection getNewConnection() throws SQLException {
        DataSource dataSource = SqlSandboxUtils.getDataSource(dbPath.getDatabaseName(), dbPath.getDatabasePath());
        return dataSource.getConnection();
    }

    private Boolean matchesConnection(SQLiteConnection sqLiteConnection) {
        return sqLiteConnection.url().contains(dbPath.getDatabasePath());
    }

    @Override
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = getNewConnection();
            } else {
                if (connection instanceof SQLiteConnection) {
                    SQLiteConnection sqLiteConnection = (SQLiteConnection) connection;
                    if (!matchesConnection(sqLiteConnection)) {
                        log.error(String.format("Had connection with path %s in StorageFactory %s",
                                sqLiteConnection.url(),
                                toString()));
                        connection = getNewConnection();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if(connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connection = null;
    }

    public void deleteDatabaseFolder() {
        SqlSandboxUtils.deleteDatabaseFolder(dbPath.getDatabaseFile());
    }

    public boolean createDatabaseFolder() {
        return new File(dbPath.getDatabaseFile()).getParentFile().mkdirs();
    }

    public String getDatabaseFileForLoggingPurposes() {
        return dbPath.getDatabaseFile();
    }
}
