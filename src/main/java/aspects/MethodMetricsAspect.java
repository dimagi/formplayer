package aspects;

import annotations.MethodMetrics;
import com.getsentry.raven.event.Breadcrumb;
import com.getsentry.raven.event.BreadcrumbBuilder;
import com.getsentry.raven.event.Event;
import com.timgroup.statsd.StatsDClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import services.RestoreFactory;
import util.Constants;
import util.FormplayerRaven;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * This aspect records various metrics for every request.
 */
@Aspect
public class MethodMetricsAspect {

    @Autowired
    protected StatsDClient datadogStatsDClient;

    @Autowired
    private FormplayerRaven raven;

    @Autowired
    private RestoreFactory restoreFactory;

    @Around(value = "@annotation(annotations.MethodMetrics)")
    public Object timeMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime();
        Object result = joinPoint.proceed();
        long timeInMs = (System.nanoTime() - startTime) / 1000000;
        String durationBucket = getDurationBucket(timeInMs);
        final Signature signature = joinPoint.getSignature();
        final MethodSignature ms = (MethodSignature) signature;
        final Method method = ms.getMethod();
        datadogStatsDClient.recordExecutionTime(
                Constants.DATADOG_TIMINGS,
                timeInMs,
                "domain:" + restoreFactory.getDomain(),
                "action:" + method.getName()
        );
        if (durationBucket.equals("lt_120s") || durationBucket.equals("over_120s")) {
            sendTimingWarningToSentry(timeInMs);
        }
        return result;
    }

    private void sendTimingWarningToSentry(final long timeInMs) {
        raven.recordBreadcrumb(new BreadcrumbBuilder() {{
            setCategory("long_request");
            setLevel(Breadcrumb.Level.WARNING);
            setData(new HashMap<String, String>() {{
                put("duration", String.format("%.3fs", timeInMs / 1000.));
            }});
        }}.build());
        raven.sendRavenException(new Exception("This request took a long time"), Event.Level.WARNING);
    }

    private static String getDurationBucket(long timeInMs) {
        long timeInS = timeInMs / 1000;
        if (timeInS < 1) {
            return "lt_001s";
        } else if (timeInS < 5) {
            return "lt_005s";
        } else if (timeInS < 20) {
            return "lt_020s";
        } else if (timeInS < 60) {
            return "lt_060s";
        } else if (timeInS < 120) {
            return "lt_120s";
        } else {
            return "over_120s";
        }
    }
}
