package org.commcare.formplayer.aspects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.util.FormplayerSentry;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

/**
 * Aspect to log the inputs and return of each API method
 */
@Aspect
@Order(4)
public class LoggingAspect {

    private final Log log = LogFactory.getLog(LoggingAspect.class);

    @Around(value = "within(org.commcare.formplayer..*) " +
            "&& @annotation(org.springframework.web.bind.annotation.RequestMapping) " +
            "&& !@annotation(org.commcare.formplayer.annotations.NoLogging)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature ms = (MethodSignature)joinPoint.getSignature();
        Method m = ms.getMethod();
        Object requestBean = null;
        String requestPath = null;
        try {
            requestPath = m.getAnnotation(RequestMapping.class).value()[0]; //Should be only one
            requestBean = joinPoint.getArgs()[0];
            log.info("Request to " + requestPath + " with bean " + requestBean);
        } catch (ArrayIndexOutOfBoundsException e) {
            // no request body
            log.info("Request to " + requestPath + " with no request body.");
        }

        if (requestBean != null && requestBean instanceof AuthenticatedRequestBean) {
            final AuthenticatedRequestBean authenticatedRequestBean =
                    (AuthenticatedRequestBean)requestBean;
            FormplayerSentry.newBreadcrumb()
                    .setData(
                            "path", requestPath,
                            "domain", authenticatedRequestBean.getDomain(),
                            "username", authenticatedRequestBean.getUsername(),
                            "restoreAs", authenticatedRequestBean.getRestoreAs(),
                            "bean", authenticatedRequestBean.toString()
                    )
                    .record();
        }

        Object result = joinPoint.proceed();
        log.info("Request to " + requestPath + " returned result " + result);
        return result;
    }
}
