package org.commcare.formplayer.exceptions;

/**
 * Used in the context of the FormDefPool
 * Is thrown when elements for a specific ID in the pool reach the set limit
 */
public class ExceedsMaxPoolSizePerId extends Exception {
    public ExceedsMaxPoolSizePerId(int maxNum, String id) {
        super("Max number (" + maxNum + ") of entries for id " + id + " exceeded.");
    }
}
