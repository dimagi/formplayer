package exceptions;

import java.io.IOException;

/**
 * Signals that there was a fault attempting to safely acquire or manage
 * the a lock around a db archive file, which prevented the success of an I/O operation
 */
public class SqlArchiveLockException extends IOException {
    public SqlArchiveLockException(String message) {
        super(message);
    }
}
