package org.commcare.formplayer.services;

import com.timgroup.statsd.StatsDClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.commcare.formplayer.util.*;

import javax.servlet.http.HttpServletRequest;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CategoryTimingHelper {
    private final Log log = LogFactory.getLog(CategoryTimingHelper.class);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private StatsDClient datadogStatsDClient;

    @Autowired
    private FormplayerSentry raven;

    public class RecordingTimer extends SimpleTimer {
        private CategoryTimingHelper parent;
        private String category, sentryMessage;

        private RecordingTimer(CategoryTimingHelper parent, String category) {
            this.parent = parent;
            this.category = category;
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
            parent.recordCategoryTiming(this, category, sentryMessage);
        }
    }

    public RecordingTimer newTimer(String category) {
        return new RecordingTimer(this, category);
    }

    public void recordCategoryTiming(Timing timing, String category) {
        recordCategoryTiming(timing, category, null);
    }
    public void recordCategoryTiming(Timing timing, String category, String sentryMessage) {
        recordCategoryTiming(timing, category, sentryMessage, null);
    }
    public void recordCategoryTiming(Timing timing, String category, String sentryMessage, String domain) {
        raven.newBreadcrumb()
                .setCategory(category)
                .setMessage(sentryMessage)
                .setData("duration", timing.formatDuration())
                .record();

        if (domain != null) {
            datadogStatsDClient.recordExecutionTime(
                Constants.DATADOG_GRANULAR_TIMINGS,
                timing.durationInMs(),
                "category:" + category,
                "request:" + RequestUtils.getRequestEndpoint(request),
                "duration:" + timing.getDurationBucket(),
                "domain:" + domain
            );
        } else {
            datadogStatsDClient.recordExecutionTime(
                Constants.DATADOG_GRANULAR_TIMINGS,
                timing.durationInMs(),
                "category:" + category,
                "request:" + RequestUtils.getRequestEndpoint(request),
                "duration:" + timing.getDurationBucket()
            );
        }

        log.debug(String.format("Timing Event[%s][%s]: %dms",
                RequestUtils.getRequestEndpoint(request),
                category,
                timing.durationInMs()));
    }
}
