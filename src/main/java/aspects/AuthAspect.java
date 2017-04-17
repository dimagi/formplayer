package aspects;

import beans.AuthenticatedRequestBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
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

    @Before(value = "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void configureAuth(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args.length > 1 && args[0] instanceof AuthenticatedRequestBean && args[1] instanceof String) {
            authService.configureAuth((AuthenticatedRequestBean) args[0], (String)args[1]);
        }
    }
}
