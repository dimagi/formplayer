package beans;

import com.fasterxml.jackson.annotation.JsonGetter;

/**
 * Created by benrudolph on 8/29/16.
 */
public class ExceptionResponseBean {
    private String exception;
    private String status;
    private String url;

    public ExceptionResponseBean() {
    }

    public ExceptionResponseBean(String exception, String url) {
        this.exception = exception;
        this.url = url;
        this.status = "error";
    }

    @JsonGetter(value = "exception")
    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
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
