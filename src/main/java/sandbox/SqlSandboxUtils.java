package sandbox;

import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

    public static void unarchiveIfArchived(String databaseName, String databasePath) throws IOException {
        String dbPath = databasePath + '/' + databaseName + ".db";
        String dbLockPath = databasePath + '/' + databaseName + ".db.lock";
        String dbGzipPath = databasePath + '/' + databaseName + ".db.gz";
        File databaseFile = new File(dbPath);
        File databaseLockFile = new File(dbLockPath);
        File databaseGzipFile = new File(dbGzipPath);

        while (databaseLockFile.exists()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (!databaseFile.exists() && databaseGzipFile.exists()) {
            try {
                boolean lockCreated = databaseLockFile.createNewFile();
                if (!lockCreated) {
                    throw new IOException("Could not create lock file");
                }
                decompressGzipFile(dbGzipPath, dbPath);
                boolean gzipDeleted = databaseGzipFile.delete();
                if (!gzipDeleted) {
                    throw new IOException("Could not delete gzipFile");
                }
            } finally {
                boolean lockDeleted = databaseLockFile.delete();
                if (!lockDeleted && databaseLockFile.exists()) {
                    throw new IOException("Could not delete lock file. This can result in a deadlock");
                }
            }
        }
    }

    private static void decompressGzipFile(String gzipFile, String newFile) throws IOException {
        // copied and modified from http://www.journaldev.com/966/java-gzip-example-compress-decompress-file
        FileInputStream fis = new FileInputStream(gzipFile);
        GZIPInputStream gis = new GZIPInputStream(fis);
        FileOutputStream fos = new FileOutputStream(newFile);
        byte[] buffer = new byte[1024];
        int len;
        while((len = gis.read(buffer)) != -1){
            fos.write(buffer, 0, len);
        }
        //close resources
        fis.close();
        fos.close();
        gis.close();
    }

    private static void compressGzipFile(String file, String gzipFile) throws IOException {
        // copied and modified from http://www.journaldev.com/966/java-gzip-example-compress-decompress-file
        FileInputStream fis = new FileInputStream(file);
        FileOutputStream fos = new FileOutputStream(gzipFile);
        GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
        byte[] buffer = new byte[1024];
        int len;
        while((len=fis.read(buffer)) != -1){
            gzipOS.write(buffer, 0, len);
        }
        //close resources
        gzipOS.close();
        fos.close();
        fis.close();
    }
}
