package org.commcare.formplayer.beans.exceptions;

import com.fasterxml.jackson.annotation.JsonGetter;

/**
 * This is the base response for an exception that occurs in Formplayer.
 */
public abstract class BaseExceptionResponseBean {
    protected String exception;
    protected String status;
    protected String url;
    protected String type;

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    @JsonGetter(value = "type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonGetter(value = "status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonGetter(value = "url")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
