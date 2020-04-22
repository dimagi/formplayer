package beans;

/**
 * Created by willpride on 1/12/16.
 */
public class NotificationMessage {

    public enum Type {
        success,
        warning,
        error
    }

    private boolean isError;
    private String type;
    private String message;

    public NotificationMessage(){
        this(null, false);
    }

    public NotificationMessage(String message, boolean isError) {
        this.message = message;
        this.isError = isError;
        if(this.isError){
            this.type = Type.error.name();
        } else {
            this.type = Type.success.name();
        }
    }

    public NotificationMessage(String message, Type type) {
        this.message = message;
        this.type = type.name();
        this.isError = type == Type.error;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("NotificationMessage message=%s, isError=%s, type=%s", message, isError, type);
    }
}