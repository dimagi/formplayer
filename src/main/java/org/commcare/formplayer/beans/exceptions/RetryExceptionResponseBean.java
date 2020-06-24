package org.commcare.formplayer.beans.exceptions;

import org.commcare.formplayer.util.Constants;

/**
 * Response bean for exception that require a retry
 */
public class RetryExceptionResponseBean extends BaseExceptionResponseBean {
    private int done;
    private int total;
    private int retryAfter;

    public RetryExceptionResponseBean(String exception, String url, int done, int total, int retryAfter) {
        this.exception = exception;
        this.url = url;
        this.status = Constants.RETRY_STATUS;
        this.done = done;
        this.total = total;
        this.retryAfter = retryAfter;
    }

    public int getRetryAfter() {
        return retryAfter;
    }

    public int getTotal() {
        return total;
    }

    public int getDone() {
        return done;
    }
}
