package org.commcare.formplayer.exceptions;

public class MultipleFormDefsFoundException extends RuntimeException {
    public MultipleFormDefsFoundException(String appId, String appVersion, String xmlns) {
        super("Found multiple form definitions with xmlns " + xmlns + ", version " + appVersion + ", and app id " + appId +
                ". Using the first form definition for now, but this issue should be investigated.");
    }
}
