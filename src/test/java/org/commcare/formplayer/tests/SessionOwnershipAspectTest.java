package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.aspectj.lang.JoinPoint;
import org.commcare.formplayer.aspects.SessionOwnershipAspect;
import org.commcare.formplayer.beans.SessionRequestBean;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.services.FormSessionService;
import org.commcare.formplayer.util.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unit tests for {@link SessionOwnershipAspect} to prevent cross-user / cross-domain
 * access to form sessions by id.
 */
public class SessionOwnershipAspectTest {

    private static final String SESSION_ID = "victim-session-id";

    private FormSessionService formSessionService;
    private SessionOwnershipAspect aspect;
    private MockHttpServletRequest request;

    @BeforeEach
    public void setUp() {
        formSessionService = mock(FormSessionService.class);
        aspect = new SessionOwnershipAspect();
        ReflectionTestUtils.setField(aspect, "formSessionService", formSessionService);

        // The victim's session, owned by "victim" in "victim_domain". Usernames are stored scrubbed.
        SerializableFormSession victimSession = mock(SerializableFormSession.class);
        when(victimSession.getUsername()).thenReturn("victim");
        when(victimSession.getDomain()).thenReturn("victim_domain");
        when(formSessionService.getSessionById(SESSION_ID)).thenReturn(victimSession);

        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private JoinPoint joinPointFor(SessionRequestBean bean) {
        JoinPoint jp = mock(JoinPoint.class);
        when(jp.getArgs()).thenReturn(new Object[]{bean, "authToken"});
        return jp;
    }

    private SessionRequestBean requestBean(String username, String domain) {
        SessionRequestBean bean = new SessionRequestBean();
        bean.setSessionId(SESSION_ID);
        bean.setUsername(username);
        bean.setDomain(domain);
        return bean;
    }

    @Test
    public void ownerMayAccessOwnSession() {
        assertDoesNotThrow(
                () -> aspect.validateSessionOwner(joinPointFor(requestBean("victim", "victim_domain"))));
    }

    @Test
    public void differentUserInSameDomainIsRefused() {
        assertThrows(FormNotFoundException.class,
                () -> aspect.validateSessionOwner(joinPointFor(requestBean("attacker", "victim_domain"))));
    }

    @Test
    public void sameUsernameInDifferentDomainIsRefused() {
        assertThrows(FormNotFoundException.class,
                () -> aspect.validateSessionOwner(joinPointFor(requestBean("victim", "attacker_domain"))));
    }

    @Test
    public void callerUsernameIsScrubbedBeforeComparison() {
        // Stored session usernames are scrubbed (TableBuilder.scrubName: '.' and '-' -> '_'); the
        // raw request username must be scrubbed the same way or every web user would be refused.
        SerializableFormSession session = mock(SerializableFormSession.class);
        when(session.getUsername()).thenReturn("jane_doe@example_com");
        when(session.getDomain()).thenReturn("victim_domain");
        when(formSessionService.getSessionById(SESSION_ID)).thenReturn(session);

        assertDoesNotThrow(() -> aspect.validateSessionOwner(
                joinPointFor(requestBean("jane.doe@example.com", "victim_domain"))));
    }

    @Test
    public void hmacAuthedRequestSkipsOwnershipCheck() {
        // HMAC (SMS / HQ-proxied) requests derive identity from the session and are trusted via the
        // shared key, so even a mismatched caller is allowed through.
        request.setAttribute(Constants.HMAC_REQUEST_ATTRIBUTE, true);
        assertDoesNotThrow(() -> aspect.validateSessionOwner(
                joinPointFor(requestBean("attacker", "attacker_domain"))));
    }

    @Test
    public void missingCredentialsAreRefused() {
        assertThrows(FormNotFoundException.class,
                () -> aspect.validateSessionOwner(joinPointFor(requestBean(null, null))));
    }
}
