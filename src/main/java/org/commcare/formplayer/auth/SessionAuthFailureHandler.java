package org.commcare.formplayer.auth;

import org.commcare.formplayer.exceptions.SessionAuthDeniedException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Answers a request whose session could not be authenticated with a 403 whose JSON body
 * says why, so a client can tell a session HQ denied (for example by IP access controls)
 * from one that is simply invalid or expired.
 *
 * {@link CommCareSessionAuthFilter} lets the filter chain continue after a failed
 * authentication, so the failure is remembered on the request (as the failure handler)
 * and reported when authorization then rejects the request (as the entry point).
 */
public class SessionAuthFailureHandler implements AuthenticationFailureHandler, AuthenticationEntryPoint {

    static final String FAILURE_ATTRIBUTE = SessionAuthFailureHandler.class.getName() + ".failure";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) {
        request.setAttribute(FAILURE_ATTRIBUTE, exception);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        Object failure = request.getAttribute(FAILURE_ATTRIBUTE);
        String reason = failure instanceof SessionAuthDeniedException denied
                ? denied.getReason()
                : SessionAuthDeniedException.REASON_SESSION_INVALID;
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(new JSONObject()
                .put("status", "error")
                .put("reason", reason)
                .toString());
    }
}
