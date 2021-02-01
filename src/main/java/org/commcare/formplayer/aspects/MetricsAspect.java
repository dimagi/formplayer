package org.commcare.formplayer.aspects;

import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import io.sentry.event.Breadcrumb;
import io.sentry.event.Event;
import com.timgroup.statsd.StatsDClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.commcare.formplayer.services.InstallService;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.services.SubmitService;
import org.commcare.formplayer.util.*;

import javax.servlet.http.HttpServletRequest;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;

/**
 * This aspect records various metrics for every request.
 */
@Aspect
@Order(1)
public class MetricsAspect {

    private static final String EXTREMELY_SLOW_REQUEST = "long_request"; // artifact of prior naming
    private static final String SLOW_REQUEST = "slow_request";

    @Autowired
    protected StatsDClient datadogStatsDClient;

    @Autowired
    private FormplayerSentry raven;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private RestoreFactory restoreFactory;

    @Autowired
    private SubmitService submitService;

    @Autowired
    private InstallService installService;

    @Around(value = "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String domain = "<unknown>";

        String requestPath = RequestUtils.getRequestEndpoint(request);
        if (args != null && args.length > 0 && args[0] instanceof AuthenticatedRequestBean) {
            AuthenticatedRequestBean bean = (AuthenticatedRequestBean) args[0];
            domain = bean.getDomain();
        }

        SimpleTimer timer = new SimpleTimer();
        timer.start();
        Object result = joinPoint.proceed();
        timer.end();

        datadogStatsDClient.increment(
                Constants.DATADOG_REQUESTS,
                "domain:" + domain,
                "request:" + requestPath,
                "duration:" + timer.getDurationBucket(),
                "unblocked_time:" + getUnblockedTimeBucket(timer),
                "blocked_time:" + getBlockedTimeBucket(),
                "restore_blocked_time:" + getRestoreBlockedTimeBucket(),
                "install_blocked_time:" + getInstallBlockedTimeBucket(),
                "submit_blocked_time:" + getSubmitBlockedTimeBucket()
        );

        datadogStatsDClient.recordExecutionTime(
                Constants.DATADOG_TIMINGS,
                timer.durationInMs(),
                "domain:" + domain,
                "request:" + requestPath,
                "duration:" + timer.getDurationBucket(),
                "unblocked_time:" + getUnblockedTimeBucket(timer),
                "blocked_time:" + getBlockedTimeBucket(),
                "restore_blocked_time:" + getRestoreBlockedTimeBucket(),
                "install_blocked_time:" + getInstallBlockedTimeBucket(),
                "submit_blocked_time:" + getSubmitBlockedTimeBucket()
        );

        long extremelySlowRequestThreshold = 60 * 1000;
        Map<String, Long> slowRequestThresholds = getSlowRequestThresholds();

        if (timer.durationInMs() >= extremelySlowRequestThreshold) {
            sendTimingWarningToSentry(timer, EXTREMELY_SLOW_REQUEST);
        } else if (slowRequestThresholds.containsKey(requestPath) && timer.durationInMs() >= slowRequestThresholds.get(requestPath)) {
            // limit slow requests sent to sentry, send 1 for every 100 requests
            int chanceOfSending = 100;
            Random random = new Random();
            if (random.nextInt(chanceOfSending) == 0) {
                sendTimingWarningToSentry(timer, SLOW_REQUEST);
            }
        }

        return result;
    }

    private String getUnblockedTimeBucket(SimpleTimer timer) {
        return Timing.getDurationBucket(timer.durationInSeconds() - getBlockedTime());
    }

    private String getBlockedTimeBucket() {
        return Timing.getDurationBucket(getRestoreBlockedTime() +
                getInstallBlockedTime() +
                getSubmitBlockedTime());
    }

    private long getBlockedTime() {
        return getRestoreBlockedTime() +
                getInstallBlockedTime() +
                getSubmitBlockedTime();
    }

    private String getRestoreBlockedTimeBucket() {
        return Timing.getDurationBucket(getRestoreBlockedTime());
    }

    private long getRestoreBlockedTime() {
        if (restoreFactory.getDownloadRestoreTimer() == null) {
            return 0;
        }
        return restoreFactory.getDownloadRestoreTimer().durationInSeconds();
    }

    private String getInstallBlockedTimeBucket() {
        return Timing.getDurationBucket(getInstallBlockedTime());
    }

    private long getInstallBlockedTime() {
        if (installService.getInstallTimer() == null) {
            return 0;
        }
        return installService.getInstallTimer().durationInSeconds();
    }

    private String getSubmitBlockedTimeBucket() {
        return Timing.getDurationBucket(getSubmitBlockedTime());
    }

    private long getSubmitBlockedTime() {
        if (submitService.getSubmitTimer() == null) {
            return 0;
        }
        return submitService.getSubmitTimer().durationInSeconds();
    }

    private Map<String, Long> getSlowRequestThresholds() {
        Map<String, Long> slowRequestThresholds = new HashMap<>();
        slowRequestThresholds.put("answer", Long.valueOf(5 * 1000));
        slowRequestThresholds.put("submit-all", Long.valueOf(20 * 1000));
        slowRequestThresholds.put("navigate_menu", Long.valueOf(20 * 1000));
        return slowRequestThresholds;
    }

    private String getSentryMessageForSlowRequest(String category) {
        Map<String, Long> messages = new HashMap<>();
        messages.put(EXTREMELY_SLOW_REQUEST, "This request took a long time");
        messages.put(SLOW_REQUEST, "This request was slow");
        if (messages.containsKey(category)) {
            return messages.get(category);
        }

        return "N/A";
    }

    private void sendTimingWarningToSentry(SimpleTimer timer, String category) {
        raven.newBreadcrumb()
                .setCategory(category)
                .setLevel(Breadcrumb.Level.WARNING)
                .setData("duration", timer.formatDuration())
                .record();
        String message = getSentryMessageForSlowRequest(category);
        raven.sendRavenException(new Exception(message), Event.Level.WARNING);
    }
}
