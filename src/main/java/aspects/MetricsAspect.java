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
@Order(2)
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
                "unblocked_time:" + getUnblockedTime(timer),
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

    private long getUnblockedTime(SimpleTimer timer) {
        return timer.durationInMs() - getBlockedTime();
    }

    private long getBlockedTime() {
        return getRestoreBlockedTime() +
                getInstallBlockedTime() +
                getSubmitBlockedTime();
    }

    private long getRestoreBlockedTime() {
        if (restoreFactory.getDownloadRestoreTimer() == null) {
            return 0;
        }
        return restoreFactory.getDownloadRestoreTimer().durationInMs();
    }

    private long getInstallBlockedTime() {
        if (installService.getInstallTimer() == null) {
            return 0;
        }
        return installService.getInstallTimer().durationInMs();
    }

    private long getSubmitBlockedTime() {
        if (submitService.getSubmitTimer() == null) {
            return 0;
        }
        return submitService.getSubmitTimer().durationInMs();
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
