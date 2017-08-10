package exceptions;

public class UserDetailsException extends RuntimeException {
    public UserDetailsException(Throwable throwable) {
        super(throwable);
    }
}
