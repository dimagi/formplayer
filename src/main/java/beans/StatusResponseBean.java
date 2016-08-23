package beans;

/**
 * A generic status response that returns either "ok" or "error" with a message
 * if it has errored.
 */
public class StatusResponseBean {
    public static final String OK = "ok";
    public static final String ERROR = "error";
    public static final String GENERIC_ERROR = "There was an error processing the request";

    private String status;
    private String message;

    public StatusResponseBean() {}

    public StatusResponseBean(Boolean success) {
        if (success) {
            status = OK;
        } else {
            status = ERROR;
            message = GENERIC_ERROR;
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
