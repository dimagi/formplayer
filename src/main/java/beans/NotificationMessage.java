package beans;

/**
 * Created by willpride on 1/12/16.
 */
public class NotificationMessage {
    private boolean isError;
    private String message;

    public NotificationMessage(){
        this(null, false);
    }

    public NotificationMessage(String message, boolean isError) {
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

    @Override
    public String toString() {
        return String.format("NotificationMessage message=%s, isError=%s", message, isError);
    }
}