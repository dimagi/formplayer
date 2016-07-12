package repo;

/**
 * A token repo accepts a String key and returns whether or not this is a valid authorization token
 */
public interface TokenRepo {
    boolean isAuthorized(String token);
}
