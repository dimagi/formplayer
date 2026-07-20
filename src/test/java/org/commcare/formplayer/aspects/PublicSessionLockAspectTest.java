package org.commcare.formplayer.aspects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.aspectj.lang.JoinPoint;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.util.RequestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Optional;

/**
 * Unit tests for {@link PublicSessionLockAspect}: a public web apps session must navigate the
 * HQ-authoritative app/endpoint, never the client-supplied values.
 */
public class PublicSessionLockAspectTest {

    private final PublicSessionLockAspect aspect = new PublicSessionLockAspect();

    private HqUserDetailsBean publicBean(String publicAppId, String publicEndpointId) {
        HqUserDetailsBean bean = new HqUserDetailsBean("domain", new String[]{"domain"}, "user",
                false, new String[]{}, new String[]{});
        bean.setPublicSession(true);
        bean.setPublicAppId(publicAppId);
        bean.setPublicEndpointId(publicEndpointId);
        return bean;
    }

    private SessionNavigationBean navBean(String appId, String endpointId, HashMap<String, String> args) {
        SessionNavigationBean bean = new SessionNavigationBean();
        bean.setAppId(appId);
        bean.setEndpointId(endpointId);
        bean.setEndpointArgs(args);
        return bean;
    }

    private JoinPoint joinPointFor(Object bean) {
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{bean, "token", null});
        return joinPoint;
    }

    @Test
    public void publicSession_overridesClientAppEndpointAndClearsArgs() {
        HashMap<String, String> args = new HashMap<>();
        args.put("case_id", "abc");
        SessionNavigationBean bean = navBean("attacker-app", "attacker-endpoint", args);

        try (MockedStatic<RequestUtils> mocked = Mockito.mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails)
                    .thenReturn(Optional.of(publicBean("real-app", "real-endpoint")));
            aspect.lockToPublicApp(joinPointFor(bean));
        }

        assertEquals("real-app", bean.getAppId());
        assertEquals("real-endpoint", bean.getEndpointId());
        assertNull(bean.getEndpointArgs());
    }

    @Test
    public void publicSession_pinsIdentityToAuthoritativeUser() {
        SessionNavigationBean bean = navBean("real-app", "real-endpoint", null);
        // Client tries to key storage off an arbitrary user; publicBean() is HQ's authoritative user.
        // username/domain plus restoreAs/restoreAsCaseId all feed FormplayerStorageFactory's sandbox
        // key (and the @UserLock key), so every one of them must be pinned, not just username/domain.
        bean.setUsername("attacker");
        bean.setDomain("attacker-domain");
        bean.setRestoreAs("victim");
        bean.setRestoreAsCaseId("victim-case-id");

        try (MockedStatic<RequestUtils> mocked = Mockito.mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails)
                    .thenReturn(Optional.of(publicBean("real-app", "real-endpoint")));
            aspect.lockToPublicApp(joinPointFor(bean));
        }

        assertEquals("user", bean.getUsername());
        assertEquals("domain", bean.getDomain());
        assertNull(bean.getRestoreAs());
        assertNull(bean.getRestoreAsCaseId());
    }

    @Test
    public void nonPublicSession_leavesBeanUntouched() {
        SessionNavigationBean bean = navBean("client-app", "client-endpoint", null);

        try (MockedStatic<RequestUtils> mocked = Mockito.mockStatic(RequestUtils.class)) {
            // publicSession defaults to false
            mocked.when(RequestUtils::getUserDetails)
                    .thenReturn(Optional.of(new HqUserDetailsBean("domain", "user")));
            aspect.lockToPublicApp(joinPointFor(bean));
        }

        assertEquals("client-app", bean.getAppId());
        assertEquals("client-endpoint", bean.getEndpointId());
    }

    @Test
    public void publicSession_missingAuthoritativeApp_failsClosed() {
        SessionNavigationBean bean = navBean("client-app", "client-endpoint", null);
        JoinPoint joinPoint = joinPointFor(bean);

        try (MockedStatic<RequestUtils> mocked = Mockito.mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails)
                    .thenReturn(Optional.of(publicBean(null, "real-endpoint")));
            assertThrows(IllegalStateException.class, () -> aspect.lockToPublicApp(joinPoint));
        }
    }

    @Test
    public void publicSession_missingAuthoritativeEndpoint_failsClosed() {
        SessionNavigationBean bean = navBean("client-app", "client-endpoint", null);
        JoinPoint joinPoint = joinPointFor(bean);

        try (MockedStatic<RequestUtils> mocked = Mockito.mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails)
                    .thenReturn(Optional.of(publicBean("real-app", null)));
            assertThrows(IllegalStateException.class, () -> aspect.lockToPublicApp(joinPoint));
        }
    }
}
