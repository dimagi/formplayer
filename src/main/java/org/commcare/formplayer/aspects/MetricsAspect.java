package org.commcare.formplayer.aspects;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

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

        String requestPath = RequestUtils.getRequestEndpoint();
        if (args != null && args.length > 0 && args[0] instanceof AuthenticatedRequestBean) {
            AuthenticatedRequestBean bean = (AuthenticatedRequestBean) args[0];
            domain = bean.getDomain();
        }

        SimpleTimer timer = new SimpleTimer();
        timer.start();
        Object result = joinPoint.proceed();
        timer.end();

        List<FormplayerDatadog.Tag> datadogArgs = new ArrayList<>();
        datadogArgs.add(new FormplayerDatadog.Tag(Constants.DOMAIN_TAG, domain));
        datadogArgs.add(new FormplayerDatadog.Tag(Constants.REQUEST_TAG, requestPath));
        datadogArgs.add(new FormplayerDatadog.Tag(Constants.DURATION_TAG, timer.getDurationBucket()));

        datadog.recordExecutionTime(Constants.DATADOG_TIMINGS, timer.durationInMs(), datadogArgs);

        FormplayerSentry.newBreadcrumb()
                .setCategory("timing")
                .setLevel(SentryLevel.WARNING)
                .setData("duration", timer.formatDuration())
                .record();

        if (timer.durationInSeconds() >= 60) {
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

    private void sendTimingWarningToSentry(SimpleTimer timer, String category) {
        String message = "N/A";
        if (sentryMessages.containsKey(category)) {
            message = sentryMessages.get(category);
        }
        Sentry.captureMessage(message, SentryLevel.WARNING);
    }
}
