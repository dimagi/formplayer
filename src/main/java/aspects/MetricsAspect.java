package aspects;

import beans.AuthenticatedRequestBean;
import com.timgroup.statsd.StatsDClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

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
        if (args!= null && args[0] instanceof AuthenticatedRequestBean) {
            AuthenticatedRequestBean bean = (AuthenticatedRequestBean) args[0];
            domain = bean.getDomain();
            user = bean.getUsernameDetail();
        }

        datadogStatsDClient.increment("requests", domain, user, requestPath);
        long startTime = System.nanoTime();
        Object result = joinPoint.proceed();
        datadogStatsDClient.gauge("timings", (System.nanoTime() - startTime) / 1000000, domain, user, requestPath);
        return result;
    }

    private String getRequestPath(ProceedingJoinPoint joinPoint) {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method m = ms.getMethod();
        return m.getAnnotation(RequestMapping.class).value()[0];
    }
}
