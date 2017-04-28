package aspects;

import beans.AuthenticatedRequestBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import services.AuthService;

import java.util.Arrays;

/**
 * Aspect to configure the AuthService
 */
@Aspect
public class AuthAspect {

    @Autowired
    protected AuthService authService;

    public void configureAuth(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof AuthenticatedRequestBean) {
            authService.configureAuth((AuthenticatedRequestBean) args[0]);
        }
    }
}
