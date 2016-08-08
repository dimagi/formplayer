package beans;

/**
 * Created by willpride on 1/12/16.
 */
public class ErrorResponseBean {
    private final String status = "error";
    private String message;

    public ErrorResponseBean(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
