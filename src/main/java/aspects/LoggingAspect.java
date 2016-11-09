package aspects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

/**
 * Aspect to log the inputs and return of each API method
 */
@Aspect
public class LoggingAspect {

    private final Log log = LogFactory.getLog(LoggingAspect.class);

    @Around(value = "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method m = ms.getMethod();
        String requestPath = m.getAnnotation(RequestMapping.class).value()[0]; //Should be only one
        try {
            Object requestBean = joinPoint.getArgs()[0];
            log.info("Request to " + requestPath + " with bean " + requestBean);
        } catch(ArrayIndexOutOfBoundsException e) {
            // no request body
            log.info("Request to " + requestPath + " with no request body.");
        }
        Object result = joinPoint.proceed();
        log.info("Request to " + requestPath + " returned result " + result);
        return result;
    }
}
