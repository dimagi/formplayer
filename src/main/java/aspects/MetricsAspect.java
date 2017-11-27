package aspects;

import beans.AuthenticatedRequestBean;
import io.sentry.event.Breadcrumb;
import io.sentry.event.Event;
import com.timgroup.statsd.StatsDClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import services.InstallService;
import services.RestoreFactory;
import services.SubmitService;
import util.*;

import javax.servlet.http.HttpServletRequest;

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

    @Around(value = "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String domain = "<unknown>";
        String user = "<unknown>";

        String requestPath = RequestUtils.getRequestEndpoint(request);
        if (args != null && args.length > 0 && args[0] instanceof AuthenticatedRequestBean) {
            AuthenticatedRequestBean bean = (AuthenticatedRequestBean) args[0];
            domain = bean.getDomain();
            user = bean.getUsernameDetail();
        }

        SimpleTimer timer = new SimpleTimer();
        timer.start();
        Object result = joinPoint.proceed();
        timer.end();

        datadogStatsDClient.increment(
                Constants.DATADOG_REQUESTS,
                "domain:" + domain,
                "user:" + user,
                "request:" + requestPath,
                "duration:" + timer.getDurationBucket()
        );

        datadogStatsDClient.recordExecutionTime(
                Constants.DATADOG_TIMINGS,
                timer.durationInMs(),
                "domain:" + domain,
                "user:" + user,
                "request:" + requestPath,
                "duration:" + timer.getDurationBucket(),
                "blocked_time:" + getBlockedTime(),
                "restore_blocked_time:" + getRestoreBlockedTime(),
                "install_blocked_time:" + getInstallBlockedTime(),
                "submit_blocked_time:" + getSubmitBlockedTime()
        );
        if (timer.durationInMs() >= 60 * 1000) {
            sendTimingWarningToSentry(timer);
        }
        return result;
    }

    private String getBlockedTime() {
        long blockedDuration = restoreFactory.getDownloadRestoreTimer().durationInMs() +
                installService.getInstallTimer().durationInMs() +
                submitService.getSubmitTimer().durationInMs();
        return "blocked_total:" + blockedDuration;
    }

    private String getRestoreBlockedTime() {
        return "restore_blocked:" + restoreFactory.getDownloadRestoreTimer().durationInMs();
    }

    private String getInstallBlockedTime() {
        return "install_blocked" + installService.getInstallTimer().durationInMs();
    }

    private String getSubmitBlockedTime() {
        return "submit_blocked" + submitService.getSubmitTimer().durationInMs();
    }

    private void sendTimingWarningToSentry(SimpleTimer timer) {
        raven.newBreadcrumb()
                .setCategory("long_request")
                .setLevel(Breadcrumb.Level.WARNING)
                .setData("duration", timer.formatDuration())
                .record();
        raven.sendSentryException(new Exception("This request took a long time"), Event.Level.WARNING);
    }
}
