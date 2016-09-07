package hq.interfaces;

/**
 * Created by benrudolph on 9/7/16.
 */
public interface CouchUser {
    boolean isAuthorized(String domain, String username);
}
