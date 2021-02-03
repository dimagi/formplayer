package org.commcare.formplayer.aspects;

import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import io.sentry.event.Breadcrumb;
import io.sentry.event.Event;
import com.timgroup.statsd.StatsDClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.commcare.formplayer.services.FormSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.services.InstallService;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.services.SubmitService;
import org.commcare.formplayer.util.*;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * This aspect records various metrics for every request.
 */
@Aspect
@Order(1)
public class MetricsAspect {

    private static final String INTOLERABLE_REQUEST = "long_request"; // artifact of prior naming
    private static final String TOLERABLE_REQUEST = "tolerable_request";

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

    @Autowired
    private FormSessionService formSessionService;

    private Map<String, Long> tolerableRequestThresholds;

    private Map<String, String> sentryMessages;

    public MetricsAspect() {
        // build slow request thresholds
        this.tolerableRequestThresholds = new HashMap<>();
        this.tolerableRequestThresholds.put("answer", Long.valueOf(5 * 1000));
        this.tolerableRequestThresholds.put("submit-all", Long.valueOf(20 * 1000));
        this.tolerableRequestThresholds.put("navigate_menu", Long.valueOf(20 * 1000));

        this.sentryMessages = new HashMap<>();
        this.sentryMessages.put(INTOLERABLE_REQUEST, "This request took a long time");
        this.sentryMessages.put(TOLERABLE_REQUEST, "This request was tolerable, but should be improved");
    }

    @Around(value = "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String domain = "<unknown>";
        String formName = null;

        SimpleTimer fetchTimer = null;
        String requestPath = RequestUtils.getRequestEndpoint(request);
        if (args != null && args.length > 0 && args[0] instanceof AuthenticatedRequestBean) {
            AuthenticatedRequestBean bean = (AuthenticatedRequestBean) args[0];
            domain = bean.getDomain();
            // only tag metrics with form_name if one of these requests
            if (requestPath.equals("submit-all")) {
                String sessionId = bean.getSessionId();
                if (sessionId != null) {
                    try {
                        fetchTimer = new SimpleTimer();
                        fetchTimer.start();
                        SerializableFormSession serializableFormSession = formSessionService.getSessionById(bean.getSessionId());
                        formName = serializableFormSession.getTitle();
                        fetchTimer.end();
                    } catch (FormNotFoundException e) {

                    }
                }
            }
        }

        SimpleTimer timer = new SimpleTimer();
        timer.start();
        Object result = joinPoint.proceed();
        timer.end();

        List<String> datadogArgs = new ArrayList<>();
        datadogArgs.add("domain:" + domain);
        datadogArgs.add("request:" + requestPath);
        datadogArgs.add("duration:" + timer.getDurationBucket());
        datadogArgs.add("unblocked_time:" + getUnblockedTimeBucket(timer));
        datadogArgs.add("blocked_time:" + getBlockedTimeBucket());
        datadogArgs.add("restore_blocked_time:" + getRestoreBlockedTimeBucket());
        datadogArgs.add("install_blocked_time:" + getInstallBlockedTimeBucket());
        datadogArgs.add("submit_blocked_time:" + getSubmitBlockedTimeBucket());

        // optional datadog args
        if (formName != null) {
            datadogArgs.add("form_name:" + formName);
        }

        datadogStatsDClient.increment(
                Constants.DATADOG_REQUESTS,
                datadogArgs.toArray(new String[datadogArgs.size()])
        );

        datadogStatsDClient.recordExecutionTime(
                Constants.DATADOG_TIMINGS,
                timer.durationInMs(),
                datadogArgs.toArray(new String[datadogArgs.size()])
        );

        if (fetchTimer != null) {
            datadogStatsDClient.recordExecutionTime(
                    "fetch_form_session",
                    fetchTimer.durationInMs(),
                    datadogArgs.toArray(new String[datadogArgs.size()])
            );
        }

        long intolerableRequestThreshold = 60 * 1000;

        if (timer.durationInMs() >= intolerableRequestThreshold) {
            sendTimingWarningToSentry(timer, INTOLERABLE_REQUEST);
        } else if (tolerableRequestThresholds.containsKey(requestPath) && timer.durationInMs() >= tolerableRequestThresholds.get(requestPath)) {
            // limit slow requests sent to sentry, send 1 for every 100 requests
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
        raven.newBreadcrumb()
                .setCategory(category)
                .setLevel(Breadcrumb.Level.WARNING)
                .setData("duration", timer.formatDuration())
                .record();

        String message = "N/A";
        if (sentryMessages.containsKey(category)) {
            message = sentryMessages.get(category);
        }
        raven.sendRavenException(new Exception(message), Event.Level.WARNING);
    }
}
