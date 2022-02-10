package org.commcare.formplayer.sandbox;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;

import org.javarosa.core.model.condition.RequestAbandonedException;
import org.commcare.formplayer.exceptions.InterruptedRuntimeException;
import org.commcare.formplayer.exceptions.SqlArchiveLockException;

public class ArchivableFile extends File {

    private static final long ARCHIVE_PROCESS_LOCK_ACQUIRE_TIMEOUT = 5 * 1000;

    private static final long ARCHIVE_PROCESS_LOCK_LIFESPAN = 5 * 60 * 1000;

    private final Log log = LogFactory.getLog(ArchivableFile.class);

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
        try {
            acquireLock(ARCHIVE_PROCESS_LOCK_ACQUIRE_TIMEOUT, true);

            if (getGzipFile().exists()) {
                gzipDeleted = getGzipFile().delete();
            }
            return super.delete() || gzipDeleted;
        } catch(IOException sqle) {
            //If we can't get the lock, no other operations will work anyway.
            return false;
        } finally {
            try {
                deleteLockOrThrow();
            } catch(IOException ioe) {
                return false;
            }
        }
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
                if (Thread.interrupted()) {
                    throw new RequestAbandonedException();
                }
            }
        }
    }

    /**
     * @param force If an existing lockfile should be deleted regardless of age
     * @return True if a lock file was expired and successfully deleted
     */
    private boolean clearLockFileIfExpired(boolean force) {
        File databaseLockFile = getLockFile();
        long lockFileAge = System.currentTimeMillis() - databaseLockFile.lastModified();
        if (force || lockFileAge > ARCHIVE_PROCESS_LOCK_LIFESPAN) {
            log.info("Evicted expired archivable file lock at: " + databaseLockFile.getPath());
            return databaseLockFile.delete() && !databaseLockFile.exists();
        }
        return false;
    }

    /**
     * @param timeout How long to wait for the lock before throwing a SqlArchiveLockException
     * @param force If true, attempts to delete an existing lockfile regardless of age
     * @throws IOException
     */
    private void acquireLock(long timeout, boolean force) throws IOException {
        File databaseLockFile = getLockFile();
        long start = System.currentTimeMillis();

        while (databaseLockFile.exists()) {
            if ((System.currentTimeMillis() - start) > timeout) {
                if (clearLockFileIfExpired(force)) {
                    break;
                }
                throw new SqlArchiveLockException("Timed out trying to acquire the archivable file lock");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new InterruptedRuntimeException(e);
            }
        }

        boolean lockCreated = databaseLockFile.createNewFile();

        if (!lockCreated) {
            throw new SqlArchiveLockException("Could not create lock file for sql archive");
        }
    }

    private void initPaths() throws IOException {
        File databaseFolder = new File(getParent());

        if (!databaseFolder.exists()) {
            Files.createDirectories(databaseFolder.toPath());
        }
    }

    public void unarchiveIfArchived() throws IOException {
        initPaths();

        File databaseFile = new File(getPath());
        File databaseGzipFile = getGzipFile();

        acquireLock(ARCHIVE_PROCESS_LOCK_ACQUIRE_TIMEOUT, false);

        try {
            if (!databaseFile.exists() && databaseGzipFile.exists()) {
                decompressGzipFile(databaseGzipFile, databaseFile);
                boolean gzipDeleted = databaseGzipFile.delete();
                if (!gzipDeleted) {
                    throw new IOException("Could not delete sql archive GZIP file");
                }
            } else if (databaseFile.exists()) {
                //This 'touch' lets the background process that cleans up older unused databases
                //recognize that the file is being used.
                databaseFile.setLastModified(System.currentTimeMillis());
            }
        } finally {
            deleteLockOrThrow();
        }
    }
    private void deleteLockOrThrow() throws IOException {
        File databaseLockFile = getLockFile();

        boolean lockDeleted = databaseLockFile.delete();
        if (!lockDeleted && databaseLockFile.exists()) {
            throw new IOException("Could not delete sql archive lock file." +
                    " This can result in a deadlock");
        }
    }
}
