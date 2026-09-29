package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.commcare.formplayer.annotations.ValidateSessionOwner;
import org.commcare.formplayer.beans.SessionRequestBean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Every route that addresses a form session by id must have {@link ValidateSessionOwner}.
 */
public class SessionOwnershipWiringTest {

    @Test
    public void everySessionKeyedRouteValidatesOwnership() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> unguarded = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("org.commcare.formplayer.application")) {
            Class<?> controller = Class.forName(bd.getBeanClassName());
            for (Method method : controller.getDeclaredMethods()) {
                // AnnotatedElementUtils.hasAnnotation ensures @PostMapping/@GetMapping is
                // also matched if ever used. Plain method inspection would miss this.
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
                    continue;
                }
                boolean sessionKeyed = Arrays.stream(method.getParameterTypes())
                        .anyMatch(SessionRequestBean.class::isAssignableFrom);
                if (sessionKeyed && !method.isAnnotationPresent(ValidateSessionOwner.class)) {
                    unguarded.add(controller.getSimpleName() + "." + method.getName());
                }
            }
        }

        assertTrue(unguarded.isEmpty(),
                "These routes take a SessionRequestBean but are missing @ValidateSessionOwner, "
                        + "leaving them open to cross-user/cross-domain session access: " + unguarded);
    }
}
