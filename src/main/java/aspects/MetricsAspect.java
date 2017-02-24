package aspects;

import beans.AuthenticatedRequestBean;
import com.timgroup.statsd.StatsDClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import util.Constants;

import java.lang.reflect.Method;

/**
 * This aspect records various metrics for every request.
 */
@Aspect
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

        datadogStatsDClient.increment(
                Constants.DATADOG_REQUESTS,
                "domain:" + domain,
                "user:" + user,
                "request:" + requestPath
        );
        long startTime = System.nanoTime();
        Object result = joinPoint.proceed();
        datadogStatsDClient.recordExecutionTime(
                Constants.DATADOG_TIMINGS,
                (System.nanoTime() - startTime) / 1000000,
                "domain:" + domain,
                "user:" + user,
                "request:" + requestPath
        );
        return result;
    }

    private String getRequestPath(ProceedingJoinPoint joinPoint) {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method m = ms.getMethod();
        return m.getAnnotation(RequestMapping.class).value()[0];
    }
}
