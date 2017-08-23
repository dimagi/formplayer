package sandbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class ArchivableFile extends File {

    public ArchivableFile(String pathname) {
        super(pathname);
    }

    @Override
    public boolean exists() {
        return super.exists() || new File(getPath() + ".gz").exists();
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

    public void unarchiveIfArchived() throws IOException {
        String dbPath = getPath();
        String dbLockPath = getPath() + ".lock";
        String dbGzipPath = getPath() + ".gz";
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
}
