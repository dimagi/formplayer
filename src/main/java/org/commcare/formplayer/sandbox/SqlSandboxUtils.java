package org.commcare.formplayer.sandbox;

import org.javarosa.core.services.Logger;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Methods that mostly are used around the mocks that replicate stuff from
 * other projects.
 *
 * @author ctsims
 * @author wspride
 */
public class SqlSandboxUtils {

    public static void deleteDatabaseFolder(String path) {
        File databaseFolder = new File(path);
        deleteDatabaseFolder(databaseFolder);
    }

    public static void deleteDatabaseFolder(File databaseFolder) {
        if (databaseFolder.exists()) {
            deleteFolder(databaseFolder);
        }
    }

    public static boolean databaseFolderExists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    public static SQLiteConnectionPoolDataSource getDataSource(File databasePath) {
        try {
            Class.forName("org.sqlite.JDBC");
            SQLiteConnectionPoolDataSource dataSource = new SQLiteConnectionPoolDataSource();
            dataSource.setUrl("jdbc:sqlite:" + databasePath.getPath() + "?journal_mode=MEMORY");
            return dataSource;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void execSql(Connection connection, String query) {
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
}
