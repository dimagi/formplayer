package org.commcare.formplayer.services;

import com.timgroup.statsd.StatsDClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.commcare.formplayer.util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CategoryTimingHelper {

    public static final String DOMAIN = "domain";
    public static final String FORM_NAME = "form_name";

    private final Log log = LogFactory.getLog(CategoryTimingHelper.class);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private StatsDClient datadogStatsDClient;

    @Autowired
    private FormplayerSentry raven;

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
            parent.recordCategoryTiming(this, category, sentryMessage, Collections.singletonMap(DOMAIN, domain));
        }
    }

    public RecordingTimer newTimer(String category) {
        return newTimer(category, null);
    }

    public RecordingTimer newTimer(String category, String domain) {
        return new RecordingTimer(this, category, domain);
    }

    public void recordCategoryTiming(Timing timing, String category) {
        recordCategoryTiming(timing, category, null);
    }
    public void recordCategoryTiming(Timing timing, String category, String sentryMessage) {
        recordCategoryTiming(timing, category, sentryMessage, null);
    }

    /**
     * @param extras - optional arguments, the following keys are accepted:
     *               CategoryTimingHelper.DOMAIN,
     *               CategoryTimingHelper.FORM_NAME,
     */
    public void recordCategoryTiming(Timing timing, String category, String sentryMessage, Map<String, String> extras) {
        raven.newBreadcrumb()
                .setCategory(category)
                .setMessage(sentryMessage)
                .setData("duration", timing.formatDuration())
                .record();

        List<String> datadogArgs = new ArrayList<>();
        datadogArgs.add("category:" + category);
        datadogArgs.add("request:" + RequestUtils.getRequestEndpoint(request));
        datadogArgs.add("duration:" + timing.getDurationBucket());
        if (extras != null) {
            if (extras.containsKey(DOMAIN)) {
                datadogArgs.add("domain:" + extras.get(DOMAIN));
            }
            if (extras.containsKey(FORM_NAME)) {
                datadogArgs.add("form_name:" + extras.get(FORM_NAME));
            }
        }

        datadogStatsDClient.recordExecutionTime(
                Constants.DATADOG_GRANULAR_TIMINGS,
                timing.durationInMs(),
                datadogArgs.toArray(new String[datadogArgs.size()])
        );


        log.debug(String.format("Timing Event[%s][%s]: %dms",
                RequestUtils.getRequestEndpoint(request),
                category,
                timing.durationInMs()));
    }
}
