package org.commcare.formplayer.aspects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.commcare.formplayer.annotations.ValidateSessionOwner;
import org.commcare.formplayer.beans.SessionRequestBean;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.services.FormSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * End-to-end check of the session ownership guard.
 *
 * <p>{@code SessionOwnershipAspectTest} exercises the check's logic directly. This
 * test confirms the check is actually connected and runs, catching mistakes a direct
 * call cannot, like a wrong or moved annotation, or the check never being registered.
 */
@SpringJUnitConfig(SessionOwnershipAspectIntegrationTest.Config.class)
public class SessionOwnershipAspectIntegrationTest {

    private static final String SESSION_ID = "victim-session-id";

    @Configuration
    @EnableAspectJAutoProxy
    static class Config {
        @Bean
        public SessionOwnershipAspect sessionOwnershipAspect() {
            return new SessionOwnershipAspect();
        }

        @Bean
        public SessionKeyedHandler sessionKeyedHandler() {
            return new SessionKeyedHandler();
        }
    }

    /** Stand-in for a controller: a bean with a {@code @ValidateSessionOwner} handler to run the check before. */
    static class SessionKeyedHandler {
        @ValidateSessionOwner
        public void act(SessionRequestBean bean) {
            // no-op; the ownership check runs before this method
        }
    }

    @MockBean
    private FormSessionService formSessionService;

    @Autowired
    private SessionKeyedHandler handler;

    @BeforeEach
    public void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        // The victim's session, owned by "victim" in "victim_domain".
        SerializableFormSession victimSession = mock(SerializableFormSession.class);
        when(victimSession.getUsername()).thenReturn("victim");
        when(victimSession.getDomain()).thenReturn("victim_domain");
        when(formSessionService.getSessionById(SESSION_ID)).thenReturn(victimSession);
    }

    @AfterEach
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private SessionRequestBean requestBean(String username, String domain) {
        SessionRequestBean bean = new SessionRequestBean();
        bean.setSessionId(SESSION_ID);
        bean.setUsername(username);
        bean.setDomain(domain);
        return bean;
    }

    @Test
    public void ownerRequestIsAllowed() {
        assertDoesNotThrow(() -> handler.act(requestBean("victim", "victim_domain")));
    }

    @Test
    public void foreignRequestIsRefused() {
        // A different user in a different domain, addressing the victim's session by id.
        assertThrows(FormNotFoundException.class,
                () -> handler.act(requestBean("attacker", "attacker_domain")));
    }
}
