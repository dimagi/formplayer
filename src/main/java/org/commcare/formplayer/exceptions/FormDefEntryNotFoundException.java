package org.commcare.formplayer.exceptions;

public class FormDefEntryNotFoundException extends Exception {
    public FormDefEntryNotFoundException(String id) {
        super("Form def for id " + id + " has not been added. Use create(...) to add new entries.");
    }
}
