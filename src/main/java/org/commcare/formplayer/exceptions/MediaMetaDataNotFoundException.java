package org.commcare.formplayer.exceptions;

public class MediaMetaDataNotFoundException extends RuntimeException {
    public MediaMetaDataNotFoundException(String id) {
        super("Could not find metadata record with formSessionId " + id + ".");
    }
}
