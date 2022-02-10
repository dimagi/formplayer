package org.commcare.formplayer.auth;

import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.services.FormSessionService;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.RequestUtils;
import org.commcare.util.JsonUtils;
import org.json.JSONObject;
import org.springframework.core.log.LogMessage;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.Builder;
import lombok.extern.apachecommons.CommonsLog;

/**
 * Request filter that performs HMAC auth if the request contains the
 * "X-MAC-DIGEST" header.
 */
@CommonsLog
@Builder
public class HmacAuthFilter extends GenericFilterBean {

    private final RequestMatcher requiresAuthenticationRequestMatcher = new AndRequestMatcher(
            new AntPathRequestMatcher("/**", HttpMethod.POST.toString()),
            new RequestHeaderRequestMatcher(Constants.HMAC_HEADER)
    );

    private final AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();

    private String hmacKey;

    private FormSessionService formSessionService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (this.requiresAuthenticationRequestMatcher.matches((HttpServletRequest)request)) {
            if (logger.isDebugEnabled()) {
                logger.debug(LogMessage
                        .of(() -> "Authenticating " + ((HttpServletRequest)request).getRequestURI()));
            }
            doAuthenticate((HttpServletRequest)request, (HttpServletResponse)response);
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace(LogMessage.format("Did not authenticate since request did not match [%s]",
                        this.requiresAuthenticationRequestMatcher));
            }
        }
        chain.doFilter(request, response);
    }

    private void doAuthenticate(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try {
            doAuthenticateInternal(request);
        } catch (AuthenticationException ex) {
            unsuccessfulAuthentication(request, response, ex);
            return;
        } catch (Exception e) {
            logger.error("Request Authorization with unexpected exception", e);
            unsuccessfulAuthentication(
                    request, response, new InternalAuthenticationServiceException("Exception checking HMAC", e)
            );
            return;
        }

        try {
            HqUserDetailsBean userDetails = getUserDetails(request);
            PreAuthenticatedAuthenticationToken authenticationResult = new PreAuthenticatedAuthenticationToken(
                    userDetails, userDetails.getDomain(), userDetails.getAuthorities());
            authenticationResult.setDetails(this.authenticationDetailsSource.buildDetails(request));
            successfulAuthentication(request, response, authenticationResult);
        } catch (BadCredentialsException ex) {
            anonymousCommcareAuthentication(request);
        } catch (AuthenticationException ex) {
            unsuccessfulAuthentication(request, response, ex);
        } catch (Exception e) {
            logger.error("Request Authorization with unexpected exception", e);
            unsuccessfulAuthentication(
                    request, response, new InternalAuthenticationServiceException("Exception getting user details", e)
            );
        }
    }

    private void doAuthenticateInternal(HttpServletRequest request) throws Exception {
        String header = request.getHeader(Constants.HMAC_HEADER);
        String hash = RequestUtils.getHmac(hmacKey, RequestUtils.getBody(request));
        if (!header.equals(hash)) {
            logger.error(LogMessage.format(
                    "Request Authorization Failed - Hash mismatch: got (%s) != expected (%s)",
                    header, hash
            ));
            throw new BadCredentialsException("Invalid HMAC hash");
        }
        request.setAttribute(Constants.HMAC_REQUEST_ATTRIBUTE, true);
    }

    private HqUserDetailsBean getUserDetails(HttpServletRequest request) {
        JSONObject body;
        try {
            body = RequestUtils.getPostData(request);
        } catch (Exception e) {
            throw new BadCredentialsException("Unable to extract user credentials from the request", e);
        }

        if (body.has("username") && body.has("domain")) {
            return new HqUserDetailsBean(
                    body.getString("domain"),
                    new String[]{body.getString("domain")},
                    body.getString("username"),
                    false,
                    JsonUtils.toArray(body.optJSONArray("enabled_toggles")),
                    JsonUtils.toArray(body.optJSONArray("enabled_previews"))
            );
        } else if (body.has("session-id")) {
            String sessionId = body.getString("session-id");
            SerializableFormSession formSession = formSessionService.getSessionById(sessionId);
            return new HqUserDetailsBean(
                    formSession.getDomain(),
                    new String[]{formSession.getDomain()},
                    formSession.getUsername(),
                    false,
                    JsonUtils.toArray(body.optJSONArray("enabled_toggles")),
                    JsonUtils.toArray(body.optJSONArray("enabled_previews"))
            );
        }
        throw new BadCredentialsException("Unable to extract user credentials from the request");
    }

    /**
     * Puts the <code>Authentication</code> instance returned by the authentication
     * manager into the secure context.
     */
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            Authentication authResult) throws IOException, ServletException {
        this.logger.debug(LogMessage.format("Authentication success: %s", authResult));
        SecurityContextHolder.getContext().setAuthentication(authResult);
    }

    /**
     * Ensures the authentication object in the secure context is set to null when
     * authentication fails.
     * <p>
     * Caches the failure exception as a request attribute
     */
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        this.logger.debug("Cleared security context due to exception", failed);
        request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, failed);
    }

    protected void anonymousCommcareAuthentication(HttpServletRequest request) {
        CommCareAnonymousAuthenticationToken token = new CommCareAnonymousAuthenticationToken();
        token.setDetails(this.authenticationDetailsSource.buildDetails(request));
        this.logger.debug("HMAC Authentication success but no user details provided");
        SecurityContextHolder.getContext().setAuthentication(token);
        this.logger.debug("Set SecurityContextHolder to CommCareAnonymousAuthentication SecurityContext");
    }

    /**
     * An anonymous token that is used to indicate valid HMAC auth but without any user details.
     * <p>
     * This is used for endpoints where we don't require user authentication but still require that the
     * request came from CommCare.
     * <p>
     * Setting the role allows us to differentiate this anonymous token from a truly anonymous one that
     * is not authenticated at all.
     */
    private static class CommCareAnonymousAuthenticationToken extends AnonymousAuthenticationToken {

        public CommCareAnonymousAuthenticationToken() {
            super("commcare", "commcare", AuthorityUtils.createAuthorityList(
                    Constants.AUTHORITY_COMMCARE));
        }
    }
}
