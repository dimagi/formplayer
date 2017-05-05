package sandbox;

import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

    public static SQLiteConnectionPoolDataSource getDataSource(String databaseName, String databasePath) {
        File databaseFolder = new File(databasePath);

        try {
            if (!databaseFolder.exists()) {
                Files.createDirectories(databaseFolder.toPath());
            }
            Class.forName("org.sqlite.JDBC");
            SQLiteConnectionPoolDataSource dataSource = new SQLiteConnectionPoolDataSource();
            dataSource.setUrl("jdbc:sqlite:" + databasePath + "/" + databaseName + ".db?journal_mode=MEMORY");
            dataSource.getConnection().setAutoCommit(false);
            return dataSource;
        } catch (ClassNotFoundException|SQLException |IOException e) {
            throw new RuntimeException(e);
        }
    }
}
