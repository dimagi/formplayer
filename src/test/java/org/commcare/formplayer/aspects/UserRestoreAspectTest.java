package org.commcare.formplayer.aspects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.commcare.formplayer.auth.DjangoAuth;
import org.commcare.formplayer.auth.HqAuth;
import org.commcare.formplayer.auth.PublicFormSessionAuth;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.util.RequestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;

/**
 * Unit tests for {@link UserRestoreAspect#getHqAuth} credential selection, in particular the
 * choice between a Django session and a public web apps session.
 */
public class UserRestoreAspectTest {

    private final UserRestoreAspect aspect = new UserRestoreAspect();

    private HqUserDetailsBean bean(boolean isPublicSession, String authToken) {
        HqUserDetailsBean bean = new HqUserDetailsBean("domain", new String[]{"domain"}, "user",
                false, new String[]{}, new String[]{});
        bean.setPublicSession(isPublicSession);
        bean.setAuthToken(authToken);
        return bean;
    }

    @Test
    public void publicSession_usesPublicFormSessionAuthWithTheSessionKey() {
        try (MockedStatic<RequestUtils> mocked = Mockito.mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails).thenReturn(Optional.of(bean(true, "pkey")));

            HqAuth auth = aspect.getHqAuth(null);

            assertTrue(auth instanceof PublicFormSessionAuth);
            assertEquals("public_form_session_key=pkey", auth.getAuthHeaders().getFirst("Cookie"));
        }
    }

    @Test
    public void publicSession_winsEvenWhenASessionidCookieIsAlsoPresent() {
        try (MockedStatic<RequestUtils> mocked = Mockito.mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails).thenReturn(Optional.of(bean(true, "pkey")));

            // Both signals present: the public credential must win, matching inbound selection.
            HqAuth auth = aspect.getHqAuth("sessionid-value");

            assertTrue(auth instanceof PublicFormSessionAuth);
        }
    }

    @Test
    public void regularSession_usesDjangoAuth() {
        try (MockedStatic<RequestUtils> mocked = Mockito.mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails).thenReturn(Optional.of(bean(false, null)));

            HqAuth auth = aspect.getHqAuth("sessionid-value");

            assertTrue(auth instanceof DjangoAuth);
        }
    }

    @Test
    public void noUserDetailsWithSessionToken_usesDjangoAuth() {
        try (MockedStatic<RequestUtils> mocked = Mockito.mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails).thenReturn(Optional.empty());

            HqAuth auth = aspect.getHqAuth("sessionid-value");

            assertTrue(auth instanceof DjangoAuth);
        }
    }

    @Test
    public void noUserDetailsNoSessionToken_returnsNull() {
        try (MockedStatic<RequestUtils> mocked = Mockito.mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails).thenReturn(Optional.empty());

            // SMS requests have neither a public session nor a sessionid cookie.
            assertNull(aspect.getHqAuth(null));
        }
    }

    @Test
    public void publicSession_restoresAsHqAuthenticatedUser_ignoringClientIdentity() throws Exception {
        RestoreFactory restoreFactory = mock(RestoreFactory.class);
        aspect.restoreFactory = restoreFactory;
        HqAuth auth = new PublicFormSessionAuth("pkey");

        // Client tries to restore as another user / case; all of it must be ignored.
        SessionNavigationBean requestBean = new SessionNavigationBean();
        requestBean.setUsername("victim");
        requestBean.setDomain("other-domain");
        requestBean.setRestoreAs("victim");
        requestBean.setRestoreAsCaseId("case-123");

        HqUserDetailsBean principal = bean(true, "pkey");
        try (MockedStatic<RequestUtils> mocked = Mockito.mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails).thenReturn(Optional.of(principal));
            aspect.configureRestoreFactory(requestBean, auth);
        }

        // Restore is pinned to the principal's username+domain with no restore-as and no case id.
        verify(restoreFactory).configure("user", "domain", null, auth);
        verify(restoreFactory, never()).configure(any(AuthenticatedRequestBean.class), any());
        verify(restoreFactory, never()).configure(anyString(), anyString(), any());
    }

    @Test
    public void regularSession_honorsRequestBeanIdentity() throws Exception {
        RestoreFactory restoreFactory = mock(RestoreFactory.class);
        aspect.restoreFactory = restoreFactory;
        HqAuth auth = new DjangoAuth("sessionid-value");

        SessionNavigationBean requestBean = new SessionNavigationBean();
        requestBean.setUsername("real-user");
        requestBean.setDomain("domain");

        try (MockedStatic<RequestUtils> mocked = Mockito.mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails).thenReturn(Optional.of(bean(false, null)));
            aspect.configureRestoreFactory(requestBean, auth);
        }

        // Unchanged: a non-public session configures the restore from the request bean itself.
        verify(restoreFactory).configure(requestBean, auth);
    }
}
