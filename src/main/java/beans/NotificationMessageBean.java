package beans;

/**
 * Created by willpride on 1/12/16.
 */
public class NotificationMessageBean {
    private boolean isError;
    private String message;

    public NotificationMessageBean(String message, boolean isError) {
        this.message = message;
        this.isError = isError;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }
}
