package org.commcare.formplayer.exceptions;

/**
 * Used in the context of the FormDefPool
 * Thrown when attempting to add a form def to the pool that is already added
 */
public class AlreadyExistsInPoolException extends Exception {
    public AlreadyExistsInPoolException() {
        super("Attempted to add form def object to pool that has already been added.");
    }
}
