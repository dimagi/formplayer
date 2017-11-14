package exceptions;

/**
 * Created by willpride on 10/5/17.
 */
public class InterruptedRuntimeException extends RuntimeException {
    public InterruptedRuntimeException(InterruptedException e) {
        super(e);
    }
}
