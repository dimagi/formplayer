package org.commcare.formplayer.application;

import org.commcare.formplayer.beans.auth.FeatureFlagChecker;
import datadog.trace.api.interceptor.TraceInterceptor;
import datadog.trace.api.interceptor.MutableSpan;
import java.util.Collection;
import java.util.Collections;

public class DatadogTraceInterceptor implements TraceInterceptor {

    @Override
    public Collection<? extends MutableSpan> onTraceComplete(Collection<? extends MutableSpan> trace) {
        if (!trace.isEmpty()) {
            MutableSpan firstSpan = trace.iterator().next();
            MutableSpan localRootSpan = firstSpan.getLocalRootSpan();
            Object apmEnabledTag = localRootSpan.getTag("feature_flag.apm_enabled");
            if (apmEnabledTag instanceof Boolean && (Boolean) apmEnabledTag) {
                return trace;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public int priority() {
        return 100;
    }
}