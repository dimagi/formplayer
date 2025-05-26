package org.commcare.formplayer.configuration;

import org.springframework.context.annotation.Configuration;
import org.commcare.formplayer.application.DatadogTraceInterceptor;
import datadog.trace.api.GlobalTracer;
import jakarta.annotation.PostConstruct;

@Configuration
public class DatadogConfig {

    @PostConstruct
    public void initializeDatadogInterceptors() {
        DatadogTraceInterceptor interceptor = new DatadogTraceInterceptor();
        GlobalTracer.get().addTraceInterceptor(interceptor);
    }
}
