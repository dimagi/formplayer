package aspects;

import beans.AuthenticatedRequestBean;
import com.getsentry.raven.event.BreadcrumbBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RequestMapping;
import util.FormplayerRaven;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Aspect to log the inputs and return of each API method
 */
@Aspect
@Order(3)
public class LoggingAspect {

    private final Log log = LogFactory.getLog(LoggingAspect.class);

    @Autowired
    private FormplayerRaven raven;

    @Around(value = "@annotation(org.springframework.web.bind.annotation.RequestMapping) " +
            "&& !@annotation(annotations.NoLogging)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method m = ms.getMethod();
        Object requestBean = null;
        final String requestPath = m.getAnnotation(RequestMapping.class).value()[0]; //Should be only one
        try {
            requestBean = joinPoint.getArgs()[0];
            log.info("Request to " + requestPath + " with bean " + requestBean);
        } catch(ArrayIndexOutOfBoundsException e) {
            // no request body
            log.info("Request to " + requestPath + " with no request body.");
        }

        if (requestBean != null && requestBean instanceof AuthenticatedRequestBean) {
            final AuthenticatedRequestBean authenticatedRequestBean = (AuthenticatedRequestBean) requestBean;
            raven.recordBreadcrumb(new BreadcrumbBuilder()
                    .setData(new HashMap<String, String> () {{
                        put("path", requestPath);
                        put("domain", authenticatedRequestBean.getDomain());
                        put("username", authenticatedRequestBean.getUsername());
                        put("restoreAs", authenticatedRequestBean.getRestoreAs());
                        put("bean", authenticatedRequestBean.toString());
                    }})
                    .build());
        }

        Object result = joinPoint.proceed();
        log.info("Request to " + requestPath + " returned result " + result);
        return result;
    }
}
