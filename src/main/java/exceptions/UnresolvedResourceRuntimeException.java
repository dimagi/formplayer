package exceptions;

/**
 * This exception is for application config exceptions that return HTML instead of text to be rendered.
 */
public class UnresolvedResourceRuntimeException extends RuntimeException {
    public UnresolvedResourceRuntimeException(Exception e) {
        super(e);
    }
    public UnresolvedResourceRuntimeException(String message) {
        super(message);
    }
}
