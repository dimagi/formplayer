package org.commcare.formplayer.aspects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.commcare.formplayer.annotations.AppInstall;
import org.commcare.formplayer.application.MenuController;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Proves {@link PublicSessionLockAspect} actually intercepts {@code @AppInstall} handlers through
 * real Spring AOP weaving. Catches a broken pointcut, missing weaving, or a lost bean.
 * Complemented by two reflection guards for the wiring the aspect depends on.
 */
@SpringJUnitConfig(PublicSessionLockAspectWeavingTest.Config.class)
public class PublicSessionLockAspectWeavingTest {

    @Configuration
    @EnableAspectJAutoProxy
    static class Config {
        @Bean
        public PublicSessionLockAspect publicSessionLockAspect() {
            return new PublicSessionLockAspect();
        }

        @Bean
        public AppInstallHandler appInstallHandler() {
            return new AppInstallHandler();
        }
    }

    /** Stand-in for a controller: a Spring bean with an {@code @AppInstall} handler to weave into. */
    static class AppInstallHandler {
        @AppInstall
        public void install(SessionNavigationBean bean) {
            // no-op; the aspect runs @Before this
        }
    }

    @Autowired
    private AppInstallHandler handler;

    private void setPublicPrincipal(String appId, String endpointId) {
        HqUserDetailsBean principal = new HqUserDetailsBean("domain", new String[]{"domain"}, "user",
                false, new String[]{}, new String[]{});
        principal.setPublicSession(true);
        principal.setPublicAppId(appId);
        principal.setPublicEndpointId(endpointId);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new PreAuthenticatedAuthenticationToken(principal, "creds",
                principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void aspectWeavesAndLocksPublicSessionToAuthoritativeAppEndpoint() {
        setPublicPrincipal("real-app", "real-endpoint");
        HashMap<String, String> args = new HashMap<>();
        args.put("case_id", "abc");
        SessionNavigationBean bean = new SessionNavigationBean();
        bean.setAppId("attacker-app");
        bean.setEndpointId("attacker-endpoint");
        bean.setEndpointArgs(args);

        // Call through the Spring proxy; if the aspect is woven it rewrites the bean @Before install.
        handler.install(bean);

        assertEquals("real-app", bean.getAppId());
        assertEquals("real-endpoint", bean.getEndpointId());
        assertNull(bean.getEndpointArgs());
    }

    @Test
    public void navigateToEndpointIsAnnotatedAppInstall() {
        boolean annotated = Arrays.stream(MenuController.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("navigateToEndpoint"))
                .anyMatch(m -> m.isAnnotationPresent(AppInstall.class));
        assertTrue(annotated,
                "get_endpoint (navigateToEndpoint) must stay @AppInstall so the lock aspect runs on it");
    }

    @Test
    public void lockAspectOrderedAfterExposeInvocationButBeforeAppInstall() {
        int lockOrder = orderOf(PublicSessionLockAspect.class);
        int appInstallOrder = orderOf(AppInstallAspect.class);
        // Must run after Spring's ExposeInvocationInterceptor (ordered HIGHEST_PRECEDENCE + 1) so the
        // advice can read the JoinPoint, and before AppInstallAspect so the sandbox keys off the
        // authoritative app id.
        assertTrue(lockOrder > Ordered.HIGHEST_PRECEDENCE + 1,
                "lock aspect must be ordered after ExposeInvocationInterceptor or it fails reading the JoinPoint");
        assertTrue(lockOrder < appInstallOrder,
                "lock aspect must run before AppInstallAspect keys the sandbox off the app id");
    }

    private static int orderOf(Class<?> aspect) {
        Order order = aspect.getAnnotation(Order.class);
        return order != null ? order.value() : Ordered.LOWEST_PRECEDENCE;
    }
}
