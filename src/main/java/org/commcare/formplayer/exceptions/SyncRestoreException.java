package org.commcare.formplayer.exceptions;

/**
 * Exception used to wrap other exceptions raised during sync / restore operations
 */
public class SyncRestoreException extends Exception {
    public SyncRestoreException(Throwable throwable) {
        super(throwable);
    }
}
