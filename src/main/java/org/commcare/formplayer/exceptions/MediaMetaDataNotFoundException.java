package org.commcare.formplayer.exceptions;

/**
 * Throw whenever a user tries to access a media metadata that does not exist.
 */
public class MediaMetaDataNotFoundException extends RuntimeException {
    public MediaMetaDataNotFoundException(String val) {
        super("Could not find metadata record with the value " + val + ".");
    }
}
