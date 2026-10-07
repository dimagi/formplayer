package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.commcare.formplayer.annotations.UserRestore;
import org.commcare.formplayer.annotations.ValidateSessionOwner;
import org.commcare.formplayer.beans.SessionRequestBean;
import org.commcare.formplayer.configuration.WebSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Every route that addresses a form session by id must have {@link ValidateSessionOwner}.
 */
public class SessionOwnershipWiringTest {

    @Test
    public void everySessionKeyedRouteValidatesOwnership() throws ClassNotFoundException {
        List<String> unguarded = new ArrayList<>();
        for (Method method : routeHandlers()) {
            if (isSessionKeyed(method) && !method.isAnnotationPresent(ValidateSessionOwner.class)) {
                unguarded.add(name(method));
            }
        }

        assertTrue(unguarded.isEmpty(),
                "These routes take a SessionRequestBean but are missing @ValidateSessionOwner, "
                        + "leaving them open to cross-user/cross-domain session access: " + unguarded);
    }

    /**
     * For a public web apps session, the ownership check relies on PublicSessionLockAspect having
     * already pinned the request bean to the authenticated principal, and that pinning only runs on
     * {@link UserRestore} handlers. So every session-keyed route a public session may reach must
     * carry {@link UserRestore}, or the check would compare against an unvalidated client value.
     */
    @Test
    public void everyPublicSessionKeyedRoutePinsIdentity() throws ClassNotFoundException {
        String[] allowlist = (String[])ReflectionTestUtils.getField(
                WebSecurityConfig.class, "PUBLIC_SESSION_ALLOWED_URLS");
        Set<String> allowed = new HashSet<>(Arrays.asList(allowlist));

        Set<String> matched = new HashSet<>();
        List<String> unpinned = new ArrayList<>();
        for (Method method : routeHandlers()) {
            Set<String> publicPaths = new HashSet<>(paths(method));
            publicPaths.retainAll(allowed);
            if (publicPaths.isEmpty()) {
                continue;
            }
            matched.addAll(publicPaths);
            if (isSessionKeyed(method) && !method.isAnnotationPresent(UserRestore.class)) {
                unpinned.add(name(method));
            }
        }

        Set<String> unmatched = new HashSet<>(allowed);
        unmatched.removeAll(matched);
        assertTrue(unmatched.isEmpty(),
                "These PUBLIC_SESSION_ALLOWED_URLS match no controller route, so this test cannot "
                        + "check them: " + unmatched);
        assertTrue(unpinned.isEmpty(),
                "These routes are reachable by a public session and take a SessionRequestBean but are "
                        + "missing @UserRestore, so the public identity is never pinned before the "
                        + "ownership check: " + unpinned);
    }

    private static List<Method> routeHandlers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<Method> handlers = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("org.commcare.formplayer.application")) {
            Class<?> controller = Class.forName(bd.getBeanClassName());
            for (Method method : controller.getDeclaredMethods()) {
                // AnnotatedElementUtils.hasAnnotation ensures @PostMapping/@GetMapping is
                // also matched if ever used. Plain method inspection would miss this.
                if (AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
                    handlers.add(method);
                }
            }
        }
        return handlers;
    }

    private static boolean isSessionKeyed(Method method) {
        return Arrays.stream(method.getParameterTypes())
                .anyMatch(SessionRequestBean.class::isAssignableFrom);
    }

    private static List<String> paths(Method method) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        List<String> paths = new ArrayList<>();
        for (String path : mapping.path()) {
            paths.add(path.startsWith("/") ? path.substring(1) : path);
        }
        return paths;
    }

    private static String name(Method method) {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }
}
