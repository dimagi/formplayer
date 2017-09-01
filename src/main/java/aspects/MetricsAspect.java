package aspects;

import beans.AuthenticatedRequestBean;
import com.timgroup.statsd.StatsDClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RequestMapping;
import util.Constants;

import java.lang.reflect.Method;

/**
 * This aspect records various metrics for every request.
 */
@Aspect
@Order(1)
public class MetricsAspect {

    @Autowired
    protected StatsDClient datadogStatsDClient;

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

        long startTime = System.nanoTime();
        Object result = joinPoint.proceed();
        long timeInMs = (System.nanoTime() - startTime) / 1000000;

        datadogStatsDClient.increment(
                Constants.DATADOG_REQUESTS,
                "domain:" + domain,
                "user:" + user,
                "request:" + requestPath,
                "duration:" + getDurationBucket(timeInMs)
        );

        datadogStatsDClient.recordExecutionTime(
                Constants.DATADOG_TIMINGS,
                timeInMs,
                "domain:" + domain,
                "user:" + user,
                "request:" + requestPath
        );
        return result;
    }

    private static String getDurationBucket(long timeInMs) {
        long timeInS = timeInMs / 1000;
        if (timeInS < 1) {
            return "lt_001s";
        } else if (timeInS <= 5) {
            return "lt_005s";
        } else if (timeInS <= 20) {
            return "lt_020s";
        } else if (timeInS <= 60) {
            return "lt_060s";
        } else if (timeInS <= 120) {
            return "lt_120s";
        } else {
            return "over_120s";
        }
    }

    static String getRequestPath(ProceedingJoinPoint joinPoint) {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method m = ms.getMethod();
        return m.getAnnotation(RequestMapping.class).value()[0];
    }
}
