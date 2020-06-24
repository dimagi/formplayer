package org.commcare.formplayer.exceptions;

/**
 * This Exception is thrown when we try to restore an asynchronous
 * restore and it fails.
 */
public class AsyncRetryException extends RuntimeException {
    private int done;
    private int total;
    private int retryAfter;

    public AsyncRetryException(String message, int done, int total, int retryAfter) {
        super(message);
        this.done = done;
        this.total = total;
        this.retryAfter = retryAfter;
    }

    public int getDone() {
        return done;
    }

    public int getTotal() {
        return total;
    }

    public int getRetryAfter() {
        return retryAfter;
    }
}
