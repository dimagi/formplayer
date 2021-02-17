package org.commcare.formplayer.exceptions;

/**
 * Thrown when the CommCare session details view returns a 403 response
 */
public class SessionAuthException extends Exception {
    private int responseCode;

    public SessionAuthException(int responseCode) {
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
