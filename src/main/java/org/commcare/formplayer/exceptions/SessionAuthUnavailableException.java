package org.commcare.formplayer.exceptions;

import javax.servlet.http.HttpServletResponse;

/**
 * Thrown when FormPlayer identifies that the user's authentication is
 * unavailable and they will need a new session to continue
 */
public class SessionAuthUnavailableException extends SessionAuthException {

    public SessionAuthUnavailableException() {
        super(HttpServletResponse.SC_BAD_REQUEST);
    }
}
