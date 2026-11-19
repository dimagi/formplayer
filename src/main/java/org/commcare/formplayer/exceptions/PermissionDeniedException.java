package org.commcare.formplayer.exceptions;

/**
 * Thrown when the authenticated user lacks permission for the requested action.
 */
public class PermissionDeniedException extends RuntimeException {
    public PermissionDeniedException(String permission) {
        super("This action requires the '" + permission + "' permission.");
    }
}
