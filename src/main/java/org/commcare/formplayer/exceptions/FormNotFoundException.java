package org.commcare.formplayer.exceptions;

/**
 * Throw whenever a user tries to access a form session that does not exist.
 */
public class FormNotFoundException extends RuntimeException {
    public FormNotFoundException(String id) {
        super("Could not find form with id " + id + ". Please ensure this form was not already submitted. " +
                "If this problem persists please report a bug through CommCareHQ.");
    }
}
