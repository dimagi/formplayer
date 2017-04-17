package aspects;

import beans.AuthenticatedRequestBean;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import services.RestoreFactory;

import java.util.Arrays;

/**
 * Aspect to configure the RestoreFactory
 */
@Aspect
public class UserRestoreAspect {

    @Autowired
    protected RestoreFactory restoreFactory;

    @Before(value = "@annotation(annotations.UserRestore)")
    public void configureRestoreFactory(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (!(args[0] instanceof AuthenticatedRequestBean)) {
            throw new RuntimeException("Could not configure RestoreFactory with args " + Arrays.toString(args));
        }
        restoreFactory.configure((AuthenticatedRequestBean)args[0]);
    }
}
