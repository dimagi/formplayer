package org.commcare.formplayer.auth;

import static org.commcare.formplayer.auth.AuthTestUtils.getMultipartRequestBuilder;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.commcare.formplayer.application.UtilController;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.configuration.WebSecurityConfig;
import org.commcare.formplayer.request.MultipleReadRequestWrappingFilter;
import org.commcare.formplayer.services.FormplayerLockRegistry;
import org.commcare.formplayer.services.HqUserDetailsService;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import jakarta.servlet.http.Cookie;


@WebMvcTest
@ContextConfiguration(classes = {
        UtilController.class,
        MockMultipartController.class,
        TestContext.class,
        WebSecurityConfig.class,
        MultipleReadRequestWrappingFilter.class,
        CacheConfiguration.class
})
public class SessionAuthTests {

    private static final String USERNAME = "citrus";
    private static final String DOMAIN = "swallowtail";
    private static final String FULL_AUTH_BODY = String.format(
            "{\"username\": \"%s\", \"domain\":\"%s\"}", USERNAME, DOMAIN);

    @Autowired
    private MockMvc mvc;

    @Autowired
    public HqUserDetailsService userDetailsService;

    @MockBean
    private FormplayerLockRegistry lockRegistry;

    @Test
    public void testEndpoint_WithoutAnyAuth_Fails() throws Exception {
        this.testEndpoint(getRequestBuilder(FULL_AUTH_BODY), status().isForbidden());
    }

    /**
     * Session cookie is present but request does not contain user details
     */
    @Test
    public void testEndpoint_WithSessionCookie_NoUserDetails_Fails() throws Exception {
        MockHttpServletRequestBuilder builder = getRequestBuilder("{}")
                .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "123"));
        this.testEndpoint(builder, status().isForbidden());
    }

    /**
     * User details request to HQ fails
     */
    @Test
    public void testEndpoint_WithFullAuth_BadCredentials_Fails() throws Exception {
        String sessionId = "123";
        TokenMatcher matcher = new TokenMatcher(DOMAIN, USERNAME, sessionId);
        when(userDetailsService.loadUserDetails(argThat(matcher))).thenThrow(
                new UsernameNotFoundException(""));
        MockHttpServletRequestBuilder builder = getRequestBuilder(FULL_AUTH_BODY)
                .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, sessionId));
        this.testEndpoint(builder, status().isForbidden());
    }

    @Test
    public void testEndpoint_WithFullAuth_Succeeds() throws Exception {
        String sessionId = "123";
        mockValidAuth(sessionId);
        MockHttpServletRequestBuilder builder = getRequestBuilder(FULL_AUTH_BODY)
                .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, sessionId));
        this.testEndpoint(builder, status().isOk());
    }

    /**
     * A public web apps request (public header + public cookie, no session cookie) authenticates
     * via a {@link PublicSessionCredential} carrying the public_form_session_key.
     */
    @Test
    public void testEndpoint_WithPublicSession_Succeeds() throws Exception {
        String sessionKey = "public-key-abc";
        mockValidPublicAuth(sessionKey);
        MockHttpServletRequestBuilder builder = getRequestBuilder(FULL_AUTH_BODY)
                .header(Constants.PUBLIC_FORM_SESSION_HEADER, Constants.PUBLIC_FORM_SESSION_HEADER_VALUE)
                .cookie(new Cookie(Constants.PUBLIC_FORM_SESSION_COOKIE_NAME, sessionKey));
        this.testEndpoint(builder, status().isOk());
    }

    /**
     * The public header is only honored when its value is exactly "true". A different value with no
     * session cookie is not a recognized session and must not authenticate.
     */
    @Test
    public void testEndpoint_PublicHeaderWrongValue_NoSessionCookie_Fails() throws Exception {
        MockHttpServletRequestBuilder builder = getRequestBuilder(FULL_AUTH_BODY)
                .header(Constants.PUBLIC_FORM_SESSION_HEADER, "false")
                .cookie(new Cookie(Constants.PUBLIC_FORM_SESSION_COOKIE_NAME, "public-key-abc"));
        this.testEndpoint(builder, status().isForbidden());
    }

    /**
     * When both signals are present, the explicit public header routes to the public credential
     * rather than the Django session cookie.
     */
    @Test
    public void testEndpoint_PublicHeaderAndSessionCookie_PrefersPublicCredential() throws Exception {
        String sessionKey = "public-key-abc";
        mockValidPublicAuth(sessionKey);
        MockHttpServletRequestBuilder builder = getRequestBuilder(FULL_AUTH_BODY)
                .header(Constants.PUBLIC_FORM_SESSION_HEADER, Constants.PUBLIC_FORM_SESSION_HEADER_VALUE)
                .cookie(new Cookie(Constants.PUBLIC_FORM_SESSION_COOKIE_NAME, sessionKey))
                .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "123"));
        this.testEndpoint(builder, status().isOk());
    }

    /**
     * A public web apps session is confined to its allowlisted routes: any other route is denied
     * with 403 at the security layer, before the controller runs. (clear_user_data is not on the
     * allowlist.)
     */
    @Test
    public void testPublicSession_DeniedOnNonAllowlistedRoute() throws Exception {
        String sessionKey = "public-key-abc";
        mockValidPublicSessionAuth(sessionKey);
        MockHttpServletRequestBuilder builder = getRequestBuilder(FULL_AUTH_BODY)
                .header(Constants.PUBLIC_FORM_SESSION_HEADER, Constants.PUBLIC_FORM_SESSION_HEADER_VALUE)
                .cookie(new Cookie(Constants.PUBLIC_FORM_SESSION_COOKIE_NAME, sessionKey));
        this.testEndpoint(builder, status().isForbidden());
    }

    /**
     * A public web apps session is permitted on only specified routes. No controller for these is
     * wired in this test, so an allowed request falls through (404) — the point is that the
     * security layer does NOT deny it with 403.
     */
    @Test
    public void testPublicSession_AllowedOnAllowlistedRoutes() throws Exception {
        String sessionKey = "public-key-abc";
        mockValidPublicSessionAuth(sessionKey);
        // get_endpoint (enter the locked form) plus every in-form action a survey/registration form
        // can invoke. None may be denied for a public session.
        for (String path : new String[]{
                Constants.URL_GET_ENDPOINT,
                Constants.URL_ANSWER_QUESTION,
                Constants.URL_ANSWER_MEDIA_QUESTION,
                Constants.URL_CLEAR_ANSWER,
                Constants.URL_NEW_REPEAT,
                Constants.URL_DELETE_REPEAT,
                Constants.URL_NEXT_INDEX,
                Constants.URL_NEXT,
                Constants.URL_PREV_INDEX,
                Constants.URL_CURRENT,
                Constants.URL_CHANGE_LANGUAGE,
                Constants.URL_GET_INSTANCE,
                Constants.URL_SUBMIT_FORM}) {
            MockHttpServletRequestBuilder builder = getRequestBuilder(path, FULL_AUTH_BODY)
                    .header(Constants.PUBLIC_FORM_SESSION_HEADER, Constants.PUBLIC_FORM_SESSION_HEADER_VALUE)
                    .cookie(new Cookie(Constants.PUBLIC_FORM_SESSION_COOKIE_NAME, sessionKey));
            this.testEndpoint(builder, result -> assertNotEquals(403, result.getResponse().getStatus(),
                    "Allowlisted route '" + path + "' must not be forbidden for a public session"));
        }
    }

    /**
     * A public session may only enter its form at the HQ-assigned endpoint via get_endpoint; it must
     * NOT be able to open an arbitrary form via new-form, which would bypass the endpoint lock.
     */
    @Test
    public void testPublicSession_DeniedOnNewForm() throws Exception {
        String sessionKey = "public-key-abc";
        mockValidPublicSessionAuth(sessionKey);
        MockHttpServletRequestBuilder builder = getRequestBuilder(Constants.URL_NEW_SESSION, FULL_AUTH_BODY)
                .header(Constants.PUBLIC_FORM_SESSION_HEADER, Constants.PUBLIC_FORM_SESSION_HEADER_VALUE)
                .cookie(new Cookie(Constants.PUBLIC_FORM_SESSION_COOKIE_NAME, sessionKey));
        this.testEndpoint(builder, status().isForbidden());
    }

    /**
     * validate_form sits outside the public allowlist, so a public session is denied there too —
     * even though validate_form's relaxed auth manager would otherwise grant any authenticated
     * caller. Regression guard for the allowlist bypass.
     */
    @Test
    public void testPublicSession_DeniedOnValidateForm() throws Exception {
        String sessionKey = "public-key-abc";
        mockValidPublicSessionAuth(sessionKey);
        MockHttpServletRequestBuilder builder = getRequestBuilder(Constants.URL_VALIDATE_FORM, FULL_AUTH_BODY)
                .header(Constants.PUBLIC_FORM_SESSION_HEADER, Constants.PUBLIC_FORM_SESSION_HEADER_VALUE)
                .cookie(new Cookie(Constants.PUBLIC_FORM_SESSION_COOKIE_NAME, sessionKey));
        this.testEndpoint(builder, status().isForbidden());
    }

    /**
     * A regular authenticated session is unaffected by the public allowlist on validate_form.
     */
    @Test
    public void testValidateForm_RegularSession_NotRestricted() throws Exception {
        String sessionId = "123";
        mockValidAuth(sessionId);
        MockHttpServletRequestBuilder builder = getRequestBuilder(Constants.URL_VALIDATE_FORM, FULL_AUTH_BODY)
                .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, sessionId));
        this.testEndpoint(builder, result -> assertNotEquals(403, result.getResponse().getStatus(),
                "validate_form must not be forbidden for a regular authenticated session"));
    }

    @Test
    public void testMultipartEndpointWithFullAuth_WithAnyHmacAuth_Succeeds() throws Exception {
        String sessionId = "123";
        mockValidAuth(sessionId);
        MockHttpServletRequestBuilder builder = getMultipartRequestBuilder(getClass(), FULL_AUTH_BODY)
                .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, sessionId));
        String hmac = "fakkydummy";
        builder.header(Constants.HMAC_HEADER, hmac);
        this.testEndpoint(builder, status().isOk());
    }

    private void mockValidAuth(String sessionId) {
        TokenMatcher matcher = new TokenMatcher(DOMAIN, USERNAME, sessionId);
        when(userDetailsService.loadUserDetails(argThat(matcher))).thenReturn(
                new HqUserDetailsBean(DOMAIN, USERNAME)
        );
    }

    // Returns a non-public bean on purpose: these tests isolate the auth filter's credential
    // routing from the route allowlist (which only restricts sessions whose bean is public).
    private void mockValidPublicAuth(String sessionKey) {
        PublicTokenMatcher matcher = new PublicTokenMatcher(DOMAIN, USERNAME, sessionKey);
        when(userDetailsService.loadUserDetails(argThat(matcher))).thenReturn(
                new HqUserDetailsBean(DOMAIN, USERNAME)
        );
    }

    private void mockValidPublicSessionAuth(String sessionKey) {
        PublicTokenMatcher matcher = new PublicTokenMatcher(DOMAIN, USERNAME, sessionKey);
        HqUserDetailsBean bean = new HqUserDetailsBean(DOMAIN, USERNAME);
        bean.setPublicSession(true);
        when(userDetailsService.loadUserDetails(argThat(matcher))).thenReturn(bean);
    }

    private void testEndpoint(MockHttpServletRequestBuilder requestBuilder,
            ResultMatcher... matchers) throws Exception {
        ResultActions actions = mvc.perform(requestBuilder)
                .andDo(log());

        for (ResultMatcher matcher : matchers) {
            actions = actions.andExpect(matcher);
        }
    }

    /**
     * Use the 'clear_user_data' endpoint for 'full auth' which required user details.
     */
    private MockHttpServletRequestBuilder getRequestBuilder(String body) {
        return getRequestBuilder(Constants.URL_CLEAR_USER_DATA, body);
    }

    private MockHttpServletRequestBuilder getRequestBuilder(String path, String body) {
        return post(String.format("/%s", path))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.csrf());
    }

    private class TokenMatcher implements ArgumentMatcher<PreAuthenticatedAuthenticationToken> {
        private String domain;
        private String username;
        private String sessionId;

        public TokenMatcher(String domain, String username, String sessionId) {
            this.domain = domain;
            this.username = username;
            this.sessionId = sessionId;
        }

        @Override
        public boolean matches(PreAuthenticatedAuthenticationToken token) {
            final UserDomainPreAuthPrincipal principal =
                    (UserDomainPreAuthPrincipal)token.getPrincipal();
            final String sessionId = (String)token.getCredentials();
            return principal.getDomain().equals(this.domain)
                    && principal.getUsername().equals(this.username)
                    && sessionId.equals(this.sessionId);
        }
    }

    private class PublicTokenMatcher implements ArgumentMatcher<PreAuthenticatedAuthenticationToken> {
        private String domain;
        private String username;
        private String sessionKey;

        public PublicTokenMatcher(String domain, String username, String sessionKey) {
            this.domain = domain;
            this.username = username;
            this.sessionKey = sessionKey;
        }

        @Override
        public boolean matches(PreAuthenticatedAuthenticationToken token) {
            if (token == null) {
                return false;
            }
            final UserDomainPreAuthPrincipal principal =
                    (UserDomainPreAuthPrincipal)token.getPrincipal();
            final Object credentials = token.getCredentials();
            return credentials instanceof PublicSessionCredential
                    && ((PublicSessionCredential)credentials).getSessionKey().equals(this.sessionKey)
                    && principal.getDomain().equals(this.domain)
                    && principal.getUsername().equals(this.username);
        }
    }
}
