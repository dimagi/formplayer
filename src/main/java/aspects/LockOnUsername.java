package aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.codehaus.jackson.map.RuntimeJsonMappingException;
import org.springframework.stereotype.Component;

/**
 * Created by benrudolph on 10/19/16.
 */
@Component
@Aspect
public class LockOnUsername {

    @Around("@annotation(annotations.LockAndBlock)")
    public Object aroundLockAndBlockOnUsername(ProceedingJoinPoint pjp) throws Throwable {
        // start stopwatch
        if (1== 1) {
            throw new RuntimeException("BEN");
        }
        Object retVal = pjp.proceed();
        // stop stopwatch
        return retVal;
    }
}
