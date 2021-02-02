package org.commcare.formplayer.aspects;

import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.beans.SessionResponseBean;
import io.sentry.event.Breadcrumb;
import io.sentry.event.Event;
import com.timgroup.statsd.StatsDClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.repo.FormSessionRepo;
import org.commcare.formplayer.services.InstallService;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.services.SubmitService;
import org.commcare.formplayer.util.*;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * This aspect records various metrics for every request.
 */
@Aspect
@Order(1)
public class MetricsAspect {

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
    protected FormSessionRepo formSessionRepo;

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
                        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(bean.getSessionId());
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

        if (timer.durationInMs() >= 60 * 1000) {
            sendTimingWarningToSentry(timer);
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

    private void sendTimingWarningToSentry(SimpleTimer timer) {
        raven.newBreadcrumb()
                .setCategory("long_request")
                .setLevel(Breadcrumb.Level.WARNING)
                .setData("duration", timer.formatDuration())
                .record();
        raven.sendRavenException(new Exception("This request took a long time"), Event.Level.WARNING);
    }
}
