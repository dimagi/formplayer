package org.commcare.formplayer.auth;

import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.RequestUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.apachecommons.CommonsLog;

/**
 * Auth filter that extracts the user details from the request (username, domain) along with the
 * session ID from the cookie.
 *
 * These are then used to authenticate the user using the HQUserDetailsService
 */
@CommonsLog
public class CommCareSessionAuthFilter extends AbstractPreAuthenticatedProcessingFilter {

    public static class SessionAuthRequestMatcher implements RequestMatcher {
        @Override
        public boolean matches(HttpServletRequest request) {
            if (HttpMethod.valueOf(request.getMethod()) != HttpMethod.POST) {
                return false;
            }
            if (request.getCookies() == null) {
                return false;
            }
            boolean hasCookie = Arrays.stream(request.getCookies()).anyMatch(
                    (cookie) -> Constants.POSTGRES_DJANGO_SESSION_ID.equals(cookie.getName())
            );
            if (!hasCookie && !isPublicSessionRequest(request)) {
                return false;
            }
            Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
            return currentUser == null;
        }

    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        if (isPublicSessionRequest(request)) {
            return new PublicSessionCredential(
                    getCookieValue(request, Constants.PUBLIC_FORM_SESSION_COOKIE_NAME));
        }
        return getCookieValue(request, Constants.POSTGRES_DJANGO_SESSION_ID);
    }

    /**
     * A public web apps request is signaled by the {@code CommCare-Public-Session: true} header
     * (a credential-routing hint, not a trust signal) paired with the
     * {@code public_form_session_key} cookie carrying the session key. Mirrors HQ's own
     * {@code _get_public_form_session} check.
     */
    private static boolean isPublicSessionRequest(HttpServletRequest request) {
        if (!Constants.PUBLIC_FORM_SESSION_HEADER_VALUE.equals(
                request.getHeader(Constants.PUBLIC_FORM_SESSION_HEADER))) {
            return false;
        }
        return getCookieValue(request, Constants.PUBLIC_FORM_SESSION_COOKIE_NAME) != null;
    }

    private static String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        try {
            JSONObject data = RequestUtils.getPostData(request);
            String username = getUsername(data);
            String domain = data.getString("domain");
            if (username != null && domain != null) {
                return new UserDomainPreAuthPrincipal(username, domain);
            }
        } catch (Exception e) {
            logger.error("Unable to extract user details from request", e);
            return null;
        }
        return null;
    }

    private String getUsername(JSONObject data) {
        try {
            return data.getString("username");
        } catch (JSONException e) {
            // TODO: Delete when no longer using HQ to proxy requests for Edit Forms
            return data.getJSONObject("session-data").getString("username");
        }
    }
}
