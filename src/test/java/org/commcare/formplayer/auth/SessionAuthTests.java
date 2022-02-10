package org.commcare.formplayer.auth;

import org.commcare.formplayer.application.UtilController;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.configuration.WebSecurityConfig;
import org.commcare.formplayer.request.MultipleReadRequestWrappingFilter;
import org.commcare.formplayer.services.FormplayerLockRegistry;
import org.commcare.formplayer.services.HqUserDetailsService;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.NotificationLogger;
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

import javax.servlet.http.Cookie;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest
@ContextConfiguration(classes = {
        UtilController.class,
        TestContext.class,
        WebSecurityConfig.class,
        MultipleReadRequestWrappingFilter.class
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
        when(userDetailsService.loadUserDetails(argThat(matcher))).thenThrow(new UsernameNotFoundException(""));
        MockHttpServletRequestBuilder builder = getRequestBuilder(FULL_AUTH_BODY)
                .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, sessionId));
        this.testEndpoint(builder, status().isForbidden());
    }

    @Test
    public void testEndpoint_WithFullAuth_Succeeds() throws Exception {
        String sessionId = "123";
        TokenMatcher matcher = new TokenMatcher(DOMAIN, USERNAME, sessionId);
        when(userDetailsService.loadUserDetails(argThat(matcher))).thenReturn(
                new HqUserDetailsBean(DOMAIN, USERNAME)
        );
        MockHttpServletRequestBuilder builder = getRequestBuilder(FULL_AUTH_BODY)
                .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, sessionId));
        this.testEndpoint(builder, status().isOk());
    }

    private void testEndpoint(MockHttpServletRequestBuilder requestBuilder, ResultMatcher... matchers) throws Exception {
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
        return post(String.format("/%s", Constants.URL_CLEAR_USER_DATA))
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
            final UserDomainPreAuthPrincipal principal = (UserDomainPreAuthPrincipal)token.getPrincipal();
            final String sessionId = (String)token.getCredentials();
            return principal.getDomain().equals(this.domain)
                    && principal.getUsername().equals(this.username)
                    && sessionId.equals(this.sessionId);
        }
    }
}
