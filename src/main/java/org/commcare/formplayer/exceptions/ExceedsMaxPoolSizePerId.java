package org.commcare.formplayer.exceptions;

public class ExceedsMaxPoolSizePerId extends Exception {
    public ExceedsMaxPoolSizePerId(int maxNum, String id) {
        super("Max number (" + maxNum + ") of entries for id " + id + " exceeded.");
    }
}
