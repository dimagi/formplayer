package org.commcare.formplayer.exceptions;

import javax.servlet.http.HttpServletResponse;

/**
 * Thrown when the CommCare session details view returns a 403 response
 */
public class SessionAuthForbiddenException extends SessionAuthException {
    public SessionAuthForbiddenException() {
        super(HttpServletResponse.SC_FORBIDDEN);
    }
}
