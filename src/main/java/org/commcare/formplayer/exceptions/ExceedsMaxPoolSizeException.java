package org.commcare.formplayer.exceptions;

/**
 * Used in the context of the FormDefPool
 * Is thrown when the entire pool reaches the set limit
 */
public class ExceedsMaxPoolSizeException extends Exception {
    public ExceedsMaxPoolSizeException(int totalSize) {
        super("Exceeds max pool size " + totalSize);
    }
}
