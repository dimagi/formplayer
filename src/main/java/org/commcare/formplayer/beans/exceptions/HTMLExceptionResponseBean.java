package org.commcare.formplayer.beans.exceptions;

import org.commcare.formplayer.util.Constants;

/**
 * For responses that return HTML
 */
public class HTMLExceptionResponseBean extends BaseExceptionResponseBean {

    public HTMLExceptionResponseBean(String exception, String url) {
        this.exception = exception;
        this.url = url;
        this.status = Constants.ERROR_STATUS;
        this.type = Constants.ERROR_TYPE_HTML;
    }
}
