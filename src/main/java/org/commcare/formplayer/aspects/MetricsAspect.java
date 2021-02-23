package org.commcare.formplayer.aspects;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.services.FormSessionService;
import org.commcare.formplayer.services.InstallService;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.services.SubmitService;
import org.commcare.formplayer.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import javax.servlet.http.HttpServletRequest;
import java.util.*;


/**
 * This aspect records various metrics for every request.
 */
@Aspect
@Order(1)
public class MetricsAspect {

    private static final String INTOLERABLE_REQUEST = "long_request"; // artifact of prior naming
    private static final String TOLERABLE_REQUEST = "tolerable_request";

    @Autowired
    private FormplayerDatadog datadog;

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

    @Autowired
    private FormSessionService formSessionService;

    private Map<String, Long> tolerableRequestThresholds;

    private Map<String, String> sentryMessages;

    public MetricsAspect() {
        // build slow request thresholds
        this.tolerableRequestThresholds = new HashMap<>();
        this.tolerableRequestThresholds.put(Constants.ANSWER_REQUEST, Long.valueOf(5 * 1000));
        this.tolerableRequestThresholds.put(Constants.SUBMIT_ALL_REQUEST, Long.valueOf(20 * 1000));
        this.tolerableRequestThresholds.put(Constants.NAV_MENU_REQUEST, Long.valueOf(20 * 1000));

        this.sentryMessages = new HashMap<>();
        this.sentryMessages.put(INTOLERABLE_REQUEST, "This request took a long time");
        this.sentryMessages.put(TOLERABLE_REQUEST, "This request was tolerable, but should be improved");
    }

    @Around(value = "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String domain = "<unknown>";

        String requestPath = RequestUtils.getRequestEndpoint(request);
        if (args != null && args.length > 0 && args[0] instanceof AuthenticatedRequestBean) {
            AuthenticatedRequestBean bean = (AuthenticatedRequestBean) args[0];
            domain = bean.getDomain();
            datadog.setDomain(domain);
        }

        SimpleTimer timer = new SimpleTimer();
        timer.start();
        Object result = joinPoint.proceed();
        timer.end();

        List<FormplayerDatadog.Tag> datadogArgs = new ArrayList<>();
        datadogArgs.add(new FormplayerDatadog.Tag(Constants.DOMAIN_TAG, domain));
        datadogArgs.add(new FormplayerDatadog.Tag(Constants.REQUEST_TAG, requestPath));
        datadogArgs.add(new FormplayerDatadog.Tag(Constants.DURATION_TAG, timer.getDurationBucket()));
        datadogArgs.add(new FormplayerDatadog.Tag(Constants.UNBLOCKED_TIME_TAG, getUnblockedTimeBucket(timer)));
        datadogArgs.add(new FormplayerDatadog.Tag(Constants.BLOCKED_TIME_TAG, getBlockedTimeBucket()));
        datadogArgs.add(new FormplayerDatadog.Tag(Constants.RESTORE_BLOCKED_TIME_TAG, getRestoreBlockedTimeBucket()));
        datadogArgs.add(new FormplayerDatadog.Tag(Constants.INSTALL_BLOCKED_TIME_TAG, getInstallBlockedTimeBucket()));
        datadogArgs.add(new FormplayerDatadog.Tag(Constants.SUBMIT_BLOCKED_TIME_TAG, getSubmitBlockedTimeBucket()));

        datadog.increment(Constants.DATADOG_REQUESTS, datadogArgs);
        datadog.recordExecutionTime(Constants.DATADOG_TIMINGS, timer.durationInMs(), datadogArgs);


        long intolerableRequestThreshold = 60 * 1000;

        if (timer.durationInMs() >= intolerableRequestThreshold) {
            sendTimingWarningToSentry(timer, INTOLERABLE_REQUEST);
        } else if (tolerableRequestThresholds.containsKey(requestPath) && timer.durationInMs() >= tolerableRequestThresholds.get(requestPath)) {
            // limit tolerable requests sent to sentry
            int chanceOfSending = 1000;
            Random random = new Random();
            if (random.nextInt(chanceOfSending) == 0) {
                sendTimingWarningToSentry(timer, TOLERABLE_REQUEST);
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

    private void sendTimingWarningToSentry(SimpleTimer timer, String category) {
        // TODO: replace with a log statement?
        raven.newBreadcrumb()
                .setCategory(category)
                .setLevel(SentryLevel.WARNING)
                .setData("duration", timer.formatDuration())
                .record();

        String message = "N/A";
        if (sentryMessages.containsKey(category)) {
            message = sentryMessages.get(category);
        }
        Sentry.captureMessage(message, SentryLevel.WARNING)
    }
}
