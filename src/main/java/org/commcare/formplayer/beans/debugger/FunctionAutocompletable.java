package org.commcare.formplayer.beans.debugger;

/**
 * Represents a function that can be used to autocomplete XPath entry.
 */
public class FunctionAutocompletable extends AutoCompletableItem {
    public FunctionAutocompletable(String functionName) {
        super(functionName + "()", functionName, "Function");
    }
}
