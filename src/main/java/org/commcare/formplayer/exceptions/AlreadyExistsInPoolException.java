package org.commcare.formplayer.exceptions;

public class AlreadyExistsInPoolException extends Exception {
    public AlreadyExistsInPoolException() {
        super("Attempted to add form def object to pool that has already been added.");
    }
}
