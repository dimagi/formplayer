package org.commcare.formplayer.aspects;

import io.opentracing.Tracer;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.JoinPoint;
import org.springframework.core.annotation.Order;
import org.commcare.formplayer.beans.auth.FeatureFlagChecker;
import datadog.trace.api.interceptor.MutableSpan;

/**
 * Aspect to tag the tracing disabled flag on the local root span
 */
@Aspect
@Order(8)
public class TagTracingDisabledAspect {

    @Before(value = "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void setValues(JoinPoint joinPoint) throws Throwable {
        final Tracer tracer = GlobalTracer.get();
        final Span span = tracer.activeSpan(); 
        boolean isToggleEnabled = FeatureFlagChecker.isToggleEnabled("ACTIVATE_DATADOG_APM_TRACES");

        if (span != null && (span instanceof MutableSpan)) {
            MutableSpan localRootSpan = ((MutableSpan) span).getLocalRootSpan();
            localRootSpan.setTag("feature_flag.apm_enabled", isToggleEnabled);
        }
    }
}