package exceptions;

/**
 * Created by benrudolph on 8/29/16.
 */
public class ApplicationConfigException extends RuntimeException {
    public ApplicationConfigException(String message) {
        super(message);
    }
}
