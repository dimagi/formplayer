package services;

import com.timgroup.statsd.StatsDClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import util.*;

import javax.servlet.http.HttpServletRequest;

@Component
public class CategoryTimingHelper {
    @Autowired
    private HttpServletRequest request;

    @Autowired
    private StatsDClient datadogStatsDClient;

    @Autowired
    private FormplayerRaven raven;

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

    public void recordCategoryTiming(SimpleTimer timer, String category, String sentryMessage) {
        raven.newBreadcrumb()
                .setCategory(category)
                .setMessage(sentryMessage)
                .setData("duration", timer.formatDuration())
                .record();

        datadogStatsDClient.recordExecutionTime(
                Constants.DATADOG_GRANULAR_TIMINGS,
                timer.durationInMs(),
                "category:" + category,
                "request:" + RequestUtils.getRequestEndpoint(request)
        );
    }
}
