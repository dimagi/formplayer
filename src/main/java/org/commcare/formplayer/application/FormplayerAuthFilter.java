package org.commcare.formplayer.application;

import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.exceptions.SessionAuthUnavailableException;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.commcare.formplayer.repo.FormSessionRepo;
import org.commcare.formplayer.services.FallbackSentryReporter;
import org.commcare.formplayer.services.HqUserDetailsService;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerHttpRequest;
import org.commcare.formplayer.util.RequestUtils;

import javax.servlet.FilterChain;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter that determines whether a request needs to be authorized,
 * then attempts authorization by checking the auth token against
 * Django's sessionid table, returning an Unauthorized response if
 * appropriate
 *
 * @author wspride
 */
@Component
public class FormplayerAuthFilter extends OncePerRequestFilter {

    @Autowired
    HqUserDetailsService userDetailsService;

    @Autowired
    FormSessionRepo formSessionRepo;

    @Autowired
    FallbackSentryReporter sentryReporter;

    @Value("${commcarehq.formplayerAuthKey}")
    private String formplayerAuthKey;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse response, FilterChain filterChain)  {
        try {
            FormplayerHttpRequest request = getRequestIfAuthorized(req);
            filterChain.doFilter(request, response);
        } catch(Exception e) {
            AuthorizationFailureException ace;
            if (e instanceof AuthorizationFailureException) {
                ace = (AuthorizationFailureException)e;
            } else {
                ace = new AuthorizationFailureException(
                        "Unexpected auth error",
                        String.format("Error configuring request authentication: %s", e.getMessage()),
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        e);
            }
            if (ace.shouldReport()) {
                logger.error(String.format("Request to %s - Authorization Failed[%d] Unexpectedly - %s",
                        req.getRequestURI(),
                        ace.getResponseCode(),
                        ace.getLog()), ace);

                sentryReporter.sendEvent(
                    sentryReporter.getEventForException(ace)
                            .withMessage(ace.getLog())
                            .withTag("uri",req.getRequestURI()));
            } else {
                logger.warn(String.format("Request to %s - Authorization Failed[%d] - %s",
                        req.getRequestURI(),
                        ace.getResponseCode(),
                        ace.getLog()));
            }
            this.setResponseError(response, ace);
        }
    }

    /**
     * Returns a FormplayerHttpRequest with appropriate authentication information if
     * authorized, or throws
     */
    private FormplayerHttpRequest getRequestIfAuthorized(HttpServletRequest req) throws AuthorizationFailureException {
        FormplayerHttpRequest request = new FormplayerHttpRequest(req);
        if (!isAuthorizationRequired(request)) {
            return request;
        }

        if (request.getHeader(Constants.HMAC_HEADER) != null && formplayerAuthKey != null) {
            String header = request.getHeader(Constants.HMAC_HEADER);
            String hash = getHmacHashFromRequest(request);
            if (!header.equals(hash)) {
                throw AuthorizationFailureException.AuthFailedWithLog (
                        "Invalid HMAC hash",
                        String.format("Hash comparison between request %s and generated %s failed", header, hash),
                        HttpServletResponse.SC_BAD_REQUEST).forceReport();

            }
            try {
                setSmsRequestDetails(request);
            } catch (FormNotFoundException e) {
                throw AuthorizationFailureException.AuthFailed("Form session not found",
                                                        HttpServletResponse.SC_NOT_FOUND);
            }
            return request;
        }
        else {
            if (getSessionId(request) == null) {
                throw AuthorizationFailureException.AuthFailed("Invalid auth session",
                        HttpServletResponse.SC_UNAUTHORIZED);
            }

            setDomain(request);

            try {
                setUserDetails(request);
            } catch(SessionAuthUnavailableException saue) {
                throw AuthorizationFailureException.AuthFailed("User session unavailable",
                        HttpServletResponse.SC_UNAUTHORIZED);
            }
            JSONObject data = RequestUtils.getPostData(request);

            if (!authorizeRequest(request, data.getString("domain"), getUsername(data))) {
                throw AuthorizationFailureException.AuthFailed("Invalid user",
                        HttpServletResponse.SC_UNAUTHORIZED);
            }
            return request;
        }
    }

    private String getHmacHashFromRequest(FormplayerHttpRequest request) throws AuthorizationFailureException {
        logger.info("Validating X-MAC-DIGEST");
        String body = null;
        try {
            body = RequestUtils.getBody(request);
            return RequestUtils.getHmac(formplayerAuthKey, body);
        } catch (Exception e) {
            throw AuthorizationFailureException.AuthFailedWithError(
                    "Error validating session",
                    String.format("Error generating HMAC hash for validation of body: %s", body == null ? "Error reading body" : body),
                    HttpServletResponse.SC_BAD_REQUEST,
                    e);
        }
    }

    private String getUsername(JSONObject data) throws AuthorizationFailureException {
        try {
            return data.getString("username");
        } catch (JSONException e) {
            try {
                // TODO: Delete when no longer using HQ to proxy requests for Edit Forms
                return data.getJSONObject("session-data").getString("username");
            } catch (JSONException etwo) {
                throw AuthorizationFailureException.AuthFailedWithError(
                        "No username for authentication",
                        "Request doesn't contain a username in POST data or session data for authentication",
                        HttpServletResponse.SC_BAD_REQUEST,
                        etwo);

            }
        }
    }

    private void setSmsRequestDetails(FormplayerHttpRequest request) {
        // If request has username and domain in body, use that
        JSONObject body = RequestUtils.getPostData(request);
        request.setRequestValidatedWithHMAC(true);
        if (body.has("username") && body.has("domain")) {
            setDomain(request);
            setSmsUserDetails(request);
        } else {
            // Otherwise, get username and domain from FormSession
            String sessionId = body.getString("session-id");
            SerializableFormSession formSession = formSessionRepo.findOneWrapped(sessionId);
            request.setDomain(formSession.getDomain());
            HqUserDetailsBean userDetailsBean = new HqUserDetailsBean(
                    new String[] {formSession.getDomain()},
                    formSession.getDomain(),
                    false
            );
            request.setUserDetails(userDetailsBean);
        }
    }

    private void setSmsUserDetails(FormplayerHttpRequest request) {
        JSONObject body = RequestUtils.getPostData(request);
        HqUserDetailsBean userDetailsBean = new HqUserDetailsBean(
                new String[] {body.getString("domain")},
                body.getString("username"),
                false
            );
        request.setUserDetails(userDetailsBean);
    }

    private void setUserDetails(FormplayerHttpRequest request) {
        request.setUserDetails(userDetailsService.getUserDetails(request.getDomain(), getSessionId(request)));

    }

    /**
     * Searches through the request cookie's to get the session id of the request.
     * @param request
     * @return The sessionid or null if not found
     */
    private String getSessionId(FormplayerHttpRequest request) {
        if(request.getCookies() !=  null) {
            for (Cookie cookie : request.getCookies()) {
                if(Constants.POSTGRES_DJANGO_SESSION_ID.equals(cookie.getName())){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void setDomain(FormplayerHttpRequest request) {
        JSONObject data = RequestUtils.getPostData(request);
        if (data.getString("domain") == null) {
            throw new RuntimeException("No domain specified for the request: " + request.getRequestURI());
        }
        request.setDomain(data.getString("domain"));
    }

    /**
     * Currently, we want to auth every POST and GET request. In particular, we want to let OPTIONS
     * requests through since these don' have auth and we need them for CORS preflight
     * @param request the request to be authorized
     * @return request needs to be authorized
     */
    private boolean isAuthorizationRequired(HttpServletRequest request){
        String uri = StringUtils.strip(request.getRequestURI(), "/");
        for (Pattern pattern : Constants.AUTH_WHITELIST) {
            Matcher matcher = pattern.matcher(uri);
            if (matcher.matches()) {
                return false;
            }
        }

        return (request.getMethod().equals("POST") || request.getMethod().equals("GET"));
    }

    /**
     * This function ensures that the request session's user and domain matches the user and domain
     * sent in the body of the POST request. Note, superusers are able to authenticate
     * as other users.
     * @param request
     * @param domain
     * @param username
     * @return true if authorized, false otherwise
     */
    private boolean authorizeRequest(FormplayerHttpRequest request, String domain, String username) {
        if (request.getUserDetails() == null) {
            return false;
        }
        return request.getUserDetails().isAuthorized(domain, username);
    }

    @Override
    public void destroy() {

    }

    public void setResponseError(HttpServletResponse response, AuthorizationFailureException authException) {
        response.setStatus(authException.getResponseCode());
        response.setContentType("application/json");

        PrintWriter writer = null;

        JSONObject responseJSON = new JSONObject();
        responseJSON.put("error", authException.getMessage());
        try {
            writer = response.getWriter();
            writer.write(responseJSON.toString());
        } catch (IOException e) {
            throw new RuntimeException("Unable to write response", e);
        } finally {
            writer.flush();
            writer.close();
        }
    }

    public static class AuthorizationFailureException extends Exception {
        private String log;
        private int responseCode;
        private boolean report;

        public String getLog() {
            return log;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public boolean shouldReport() {
            return report;
        }

        public static AuthorizationFailureException AuthFailed(String message, int responseCode) {
            return new AuthorizationFailureException(message, message, responseCode, null);
        }
        public static AuthorizationFailureException AuthFailedWithLog(String message, String log, int responseCode) {
            return new AuthorizationFailureException(message, log, responseCode, null);
        }
        public static AuthorizationFailureException AuthFailedWithError(String message, String log, int responseCode, Exception e) {
            return new AuthorizationFailureException(message, log, responseCode, e);
        }
        private AuthorizationFailureException(String message, String log,
                                              int responseCode,
                                              Exception source) {
            super(message);
            this.log = log;
            this.responseCode = responseCode;
            if (source != null) {
                this.report = true;
                this.initCause(source);
            }
        }

        public AuthorizationFailureException forceReport() {
            this.report = true;
            return this;
        }
    }
}