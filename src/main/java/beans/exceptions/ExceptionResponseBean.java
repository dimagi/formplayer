package beans.exceptions;

import util.Constants;

/**
 * This class is a generic response for an exception raised in Formplayer
 */
public class ExceptionResponseBean extends BaseExceptionResponseBean {

    public ExceptionResponseBean() {
    }

    public ExceptionResponseBean(String exception, String url) {
        this.exception = exception;
        this.url = url;
        this.status = Constants.ERROR_STATUS;
        this.type = Constants.ERROR_TYPE_TEXT;
    }
}
