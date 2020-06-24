package org.commcare.formplayer.exceptions;

/**
 * Throw whenever a user tries to access a form session that does not exist.
 */
public class MenuNotFoundException extends RuntimeException {
    public MenuNotFoundException(String id) {
        super("Could not find menu session with ID " + id + ", likely because it has been completed. " +
                "Redirecting to home screen. If this issue persists, please file a bug report.");
    }
}
