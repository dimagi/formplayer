package org.commcare.formplayer.exceptions;

import org.springframework.security.core.AuthenticationException;

/**
 * Thrown when HQ refuses to authenticate a session for a stated reason (for example,
 * IP access controls), as opposed to the session being unavailable. The reason is
 * passed through to the client in the auth failure response.
 */
public class SessionAuthDeniedException extends AuthenticationException {

    public static final String REASON_UNKNOWN = "unknown";
    public static final String REASON_SESSION_INVALID = "session_invalid";

    private final String reason;

    public SessionAuthDeniedException(String reason) {
        super("Session auth denied: " + reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
