package org.commcare.formplayer.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.util.RequestUtils;
import org.commcare.modern.database.TableBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;

/**
 * Unit tests for {@link FormSessionFactory#verifyPublicSessionOwnership}: a public web apps session
 * may only load the form session its own one-time link created, never another user's by id.
 */
public class FormSessionFactoryTest {

    private final FormSessionFactory factory = new FormSessionFactory();

    private HqUserDetailsBean publicBean(String username, String domain) {
        HqUserDetailsBean bean = new HqUserDetailsBean(domain, new String[]{domain}, username,
                false, new String[]{}, new String[]{});
        bean.setPublicSession(true);
        return bean;
    }

    private SerializableFormSession session(String username, String domain) {
        SerializableFormSession session = mock(SerializableFormSession.class);
        // Sessions persist the scrubbed username (see FormSession's new-session constructor).
        when(session.getUsername()).thenReturn(TableBuilder.scrubName(username));
        when(session.getDomain()).thenReturn(domain);
        when(session.getId()).thenReturn("session-id");
        return session;
    }

    @Test
    public void publicSession_cannotLoadAnotherUsersFormSession() {
        SerializableFormSession victimSession = session("victim@domain", "domain");
        try (MockedStatic<RequestUtils> mocked = mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails)
                    .thenReturn(Optional.of(publicBean("public_abc@domain", "domain")));
            assertThrows(FormNotFoundException.class,
                    () -> factory.verifyPublicSessionOwnership(victimSession));
        }
    }

    @Test
    public void publicSession_cannotLoadFormSessionInAnotherDomain() {
        SerializableFormSession otherDomainSession = session("public_abc@other", "other");
        try (MockedStatic<RequestUtils> mocked = mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails)
                    .thenReturn(Optional.of(publicBean("public_abc@domain", "domain")));
            assertThrows(FormNotFoundException.class,
                    () -> factory.verifyPublicSessionOwnership(otherDomainSession));
        }
    }

    @Test
    public void publicSession_canLoadItsOwnFormSession() {
        SerializableFormSession ownSession = session("public_abc@domain", "domain");
        try (MockedStatic<RequestUtils> mocked = mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails)
                    .thenReturn(Optional.of(publicBean("public_abc@domain", "domain")));
            // Its own session passes the check (no exception).
            factory.verifyPublicSessionOwnership(ownSession);
        }
    }

    @Test
    public void nonPublicSession_ownershipCheckIsSkipped() {
        // A regular session is unaffected: the check only constrains public sessions.
        SerializableFormSession anySession = session("someone-else@domain", "domain");
        try (MockedStatic<RequestUtils> mocked = mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails)
                    .thenReturn(Optional.of(new HqUserDetailsBean("domain", "user")));
            factory.verifyPublicSessionOwnership(anySession);
        }
    }

    @Test
    public void noAuthenticatedUser_ownershipCheckIsSkipped() {
        // Non-request / unauthenticated contexts (e.g. purge tasks) must not be blocked.
        SerializableFormSession anySession = session("someone@domain", "domain");
        try (MockedStatic<RequestUtils> mocked = mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails).thenReturn(Optional.empty());
            factory.verifyPublicSessionOwnership(anySession);
        }
    }
}
