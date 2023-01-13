package org.commcare.formplayer.exceptions;

/**
 * Throw when the data instance with the given key doesn't exist in the DB
 */
public class InstanceNotFoundException extends RuntimeException {

    public InstanceNotFoundException(String key, String namespace) {
        super(String.format(
                "Could not find data instance with ID %s (namespace=%s)." +
                "Redirecting to home screen. If this issue persists, please file a bug report.",
                key, namespace
        ));
    }
}
