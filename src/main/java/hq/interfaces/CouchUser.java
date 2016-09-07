package hq.interfaces;

/**
 * An interface the represents an HQ CouchUser
 */
public interface CouchUser {
    boolean isAuthorized(String domain, String username);
}
