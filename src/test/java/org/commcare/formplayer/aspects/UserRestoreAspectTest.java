package org.commcare.formplayer.aspects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.commcare.formplayer.auth.DjangoAuth;
import org.commcare.formplayer.auth.HqAuth;
import org.commcare.formplayer.auth.PublicFormSessionAuth;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
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
}
