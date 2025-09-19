package org.commcare.formplayer.beans.exceptions;

import org.commcare.formplayer.util.Constants;

/**
 * This class is a generic response for an exception raised in Formplayer
 */
public class ExceptionResponseBean extends BaseExceptionResponseBean {

    public ExceptionResponseBean() {
    }

    public ExceptionResponseBean(String exception, String url) {
        this(exception, url, Constants.ERROR_TYPE_TEXT);
    }

    public ExceptionResponseBean(String exception, String url, String type) {
        this.exception = exception;
        this.url = url;
        this.status = Constants.ERROR_STATUS;
        this.type = type;
    }


    public ExceptionResponseBean(String exception, String url, Integer statusCode) {
        this.exception = exception;
        this.url = url;
        this.status = Constants.ERROR_STATUS;
        this.type = Constants.ERROR_TYPE_TEXT;
        this.statusCode = statusCode;
    }
}
