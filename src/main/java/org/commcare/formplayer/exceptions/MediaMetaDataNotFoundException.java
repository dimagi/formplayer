package org.commcare.formplayer.exceptions;

public class MediaMetaDataNotFoundException extends RuntimeException {
    public MediaMetaDataNotFoundException(String val) {
        super("Could not find metadata record with the value " + val + ".");
    }
}
