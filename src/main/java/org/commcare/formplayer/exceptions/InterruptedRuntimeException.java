package org.commcare.formplayer.exceptions;

/**
 * A RuntimeException to wrap InterruptedException, thrown when Thread is interrupted while sleeping or waiting
 */
public class InterruptedRuntimeException extends RuntimeException {
    public InterruptedRuntimeException(InterruptedException e) {
        super(e);
    }
}
