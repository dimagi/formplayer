package aspects;

import beans.AuthenticatedRequestBean;
import com.getsentry.raven.event.Breadcrumb;
import com.getsentry.raven.event.Event;
import com.timgroup.statsd.StatsDClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RequestMapping;
import util.Constants;
import util.FormplayerRaven;
import util.SimpleTimer;

import java.lang.reflect.Method;

/**
 * This aspect records various metrics for every request.
 */
@Aspect
@Order(1)
public class MetricsAspect {

    @Autowired
    protected StatsDClient datadogStatsDClient;

    @Autowired
    private FormplayerRaven raven;

    @Around(value = "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String domain = "<unknown>";
        String user = "<unknown>";

        String requestPath = getRequestPath(joinPoint);
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
                "duration:" + timer.getDurationBucket()
        );
        if (timer.durationInMs() >= 60 * 1000) {
            sendTimingWarningToSentry(timer);
        }
        return result;
    }

    private void sendTimingWarningToSentry(SimpleTimer timer) {
        raven.newBreadcrumb()
                .setCategory("long_request")
                .setLevel(Breadcrumb.Level.WARNING)
                .setData("duration", timer.formatDuration())
                .record();
        raven.sendRavenException(new Exception("This request took a long time"), Event.Level.WARNING);
    }

    static String getRequestPath(ProceedingJoinPoint joinPoint) {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method m = ms.getMethod();
        return m.getAnnotation(RequestMapping.class).value()[0];
    }
}
