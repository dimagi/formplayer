package org.commcare.formplayer.sandbox;

import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.io.File;

/**
 * Methods that mostly are used around the mocks that replicate stuff from
 * other projects.
 *
 * @author ctsims
 * @author wspride
 */
public class SqlSandboxUtils {

    public static String optionsString;

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
            dataSource.setUrl("jdbc:sqlite:" + databasePath.getPath() + optionsString);
            return dataSource;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
