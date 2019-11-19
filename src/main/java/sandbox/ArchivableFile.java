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

    private File getGzipFile() {
        return new File(getPath() + ".gz");
    }

    private File getLockFile() {
        return new File(getPath() + ".lock");
    }

    @Override
    public boolean exists() {
        return super.exists() || getGzipFile().exists();
    }

    @Override
    public boolean delete() {
        boolean gzipDeleted = false;
        waitOnLock();
        if (getGzipFile().exists()) {
            gzipDeleted = getGzipFile().delete();
        }
        return super.delete() || gzipDeleted;
    }

    private static void decompressGzipFile(File gzipFile, File newFile) throws IOException {
        // copied and modified from http://www.journaldev.com/966/java-gzip-example-compress-decompress-file
        try (
        FileInputStream fis = new FileInputStream(gzipFile);
        GZIPInputStream gis = new GZIPInputStream(fis);
        FileOutputStream fos = new FileOutputStream(newFile);
        ) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
    }

    private void waitOnLock() {
        while (getLockFile().exists()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void unarchiveIfArchived() throws IOException {
        File databaseFile = new File(getPath());
        File databaseLockFile = getLockFile();
        File databaseGzipFile = getGzipFile();

        waitOnLock();

        if (!databaseFile.exists() && databaseGzipFile.exists()) {
            try {
                boolean lockCreated = databaseLockFile.createNewFile();
                if (!lockCreated) {
                    throw new IOException("Could not create lock file");
                }
                decompressGzipFile(databaseGzipFile, databaseFile);
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
