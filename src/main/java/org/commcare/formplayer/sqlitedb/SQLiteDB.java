package org.commcare.formplayer.sqlitedb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sqlite.SQLiteConnection;
import org.commcare.formplayer.sandbox.ArchivableFile;
import org.commcare.formplayer.sandbox.SqlSandboxUtils;
import org.commcare.formplayer.services.ConnectionHandler;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class SQLiteDB implements ConnectionHandler {
    private DBPath dbPath;
    private ArchivableFile dbArchivableFile;
    private final Log log = LogFactory.getLog(SQLiteDB.class);
    private Connection connection;

    public SQLiteDB(DBPath dbPath) {
        this.dbPath = dbPath;
        /*
           FormplayerStorageFactory and RestoreFactory are instantiated with sqLiteDB = SQLLiteDB(null)
           and sqLiteDB is only set to a real value during .configure;
           this doesn't make a lot of sense to me, but appears to be required for tests.
        */
        if (dbPath != null) {
            dbArchivableFile = new ArchivableFile(dbPath.getDatabaseFile());
        }
    }

    private Connection getNewConnection() throws SQLException {
        try {
            dbArchivableFile.unarchiveIfArchived();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DataSource dataSource = SqlSandboxUtils.getDataSource(dbArchivableFile);
        return dataSource.getConnection();
    }

    private Boolean matchesConnection(SQLiteConnection sqLiteConnection) {
        return sqLiteConnection.getUrl().contains(dbPath.getDatabasePath());
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
                        log.error(String.format("Connection for path %s already exists",  sqLiteConnection.getUrl()));
                        connection.close();
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

    public void deleteDatabaseFile() {
        closeConnection();
        SqlSandboxUtils.deleteDatabaseFolder(dbArchivableFile);
    }

    public void deleteDatabaseFolder() {
        SqlSandboxUtils.deleteDatabaseFolder(dbArchivableFile.getParentFile());
    }

    public boolean createDatabaseFolder() {
        return dbArchivableFile.getParentFile().mkdirs();
    }

    public boolean databaseFileExists() {
        return dbArchivableFile.exists();
    }

    public boolean databaseFolderExists() {
        return dbArchivableFile.getParentFile().exists();
    }

    public String getDatabaseFileForDebugPurposes() {
        return dbPath.getDatabaseFile();
    }
}
