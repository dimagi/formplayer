package org.commcare.formplayer.exceptions;

public class ExceedsMaxPoolSizeException extends Exception {
    public ExceedsMaxPoolSizeException(int totalSize) {
        super("Exceeds max pool size " + totalSize);
    }
}
