package org.commcare.formplayer.exceptions;

import java.util.UUID;

/**
 * Throw when the data instance with the given key doesn't exist in the DB
 */
public class InstanceNotFoundException extends RuntimeException {

    public InstanceNotFoundException(UUID key) {
        super("Could not find data instance with ID " + key + "." +
                "Redirecting to home screen. If this issue persists, please file a bug report.");
    }
}
