package org.commcare.formplayer.exceptions;

/**
 * Used in the context of the FormDefPool
 * Is thrown when attempting to add a form def to the pool
 */
public class FormDefEntryNotFoundException extends Exception {
    public FormDefEntryNotFoundException(String id) {
        super("Form def for id " + id + " has not been added. Use create(...) to add new objects to the pool.");
    }
}
