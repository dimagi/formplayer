package org.commcare.formplayer.services;

import com.timgroup.statsd.StatsDClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerSentry;
import org.commcare.formplayer.util.RequestUtils;
import org.commcare.formplayer.util.SimpleTimer;
import org.commcare.formplayer.util.Timing;
import org.commcare.formplayer.utils.CheckedRunnable;
import org.commcare.formplayer.utils.CheckedSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CategoryTimingHelper {

    private final Log log = LogFactory.getLog(CategoryTimingHelper.class);

    @Autowired
    private StatsDClient datadogStatsDClient;

    public class RecordingTimer extends SimpleTimer {
        private CategoryTimingHelper parent;
        private String category, sentryMessage, domain;

        private RecordingTimer(CategoryTimingHelper parent, String category, String domain) {
            this.parent = parent;
            this.category = category;
            this.domain = domain;
        }

        @Override
        public RecordingTimer end() {
            super.end();
            return this;
        }

        public RecordingTimer setMessage(String message) {
            this.sentryMessage = message;
            return this;
        }

        public void record() {
            parent.recordCategoryTiming(this, category, sentryMessage, Collections.singletonMap(Constants.DOMAIN_TAG, domain));
        }
    }

    public RecordingTimer newTimer(String category) {
        return newTimer(category, null);
    }

    public RecordingTimer newTimer(String category, String domain) {
        return new RecordingTimer(this, category, domain);
    }

    private void logTiming(Timing timing, String category) {
        log.debug(String.format("Timing Event[%s][%s]: %dms",
                RequestUtils.getRequestEndpoint(),
                category,
                timing.durationInMs()));
    }

    @SneakyThrows
    public void timed(String category, CheckedRunnable timed) {
        timed(category, timed, null);
    }

    @SneakyThrows
    public void timed(String category, CheckedRunnable timed, Map<String, String> extras) {
        timed(category, () -> {
            timed.run();
            return null;
        }, extras);
    }

    @SneakyThrows
    public <T> T timed(String category, CheckedSupplier<T> timed) {
        return timed(category, timed, null);
    }

    @SneakyThrows
    public <T> T timed(String category, CheckedSupplier<T> timed, Map<String, String> extras) {
        SimpleTimer timer = new SimpleTimer();
        timer.start();
        try {
            return timed.get();
        } finally {
            timer.end();
            recordCategoryTiming(timer, category, null, extras);
            logTiming(timer, category);
        }
    }

    /**
     * @param extras - optional tag/value pairs to send to datadog
     *               NOTE: if adding a new tag, add a constant for the tag name
     */
    public void recordCategoryTiming(Timing timing, String category, String sentryMessage, Map<String, String> extras) {
        FormplayerSentry.recordTimingBreadcrumb(timing, category, sentryMessage);
        recordDatadogMetrics(timing, category, extras);
        logTiming(timing, category);
    }

    private void recordDatadogMetrics(Timing timing, String category, Map<String, String> extras) {
        List<String> datadogArgs = new ArrayList<>();
        datadogArgs.add(Constants.CATEGORY_TAG + ":" + category);
        datadogArgs.add(Constants.REQUEST_TAG + ":" + RequestUtils.getRequestEndpoint());
        datadogArgs.add(Constants.DURATION_TAG + ":" + timing.getDurationBucket());
        if (extras != null) {
            for (Map.Entry<String, String> entry : extras.entrySet()) {
                datadogArgs.add(entry.getKey() + ":" + entry.getValue());
            }
        }

        datadogStatsDClient.recordExecutionTime(
                Constants.DATADOG_GRANULAR_TIMINGS,
                timing.durationInMs(),
                datadogArgs.toArray(new String[datadogArgs.size()])
        );
    }
}
