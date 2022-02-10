package org.commcare.formplayer.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.services.BrowserValuesProvider;
import org.javarosa.core.model.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

/**
 * Created by amstone326 on 1/8/18.
 */
@Aspect
@Order(3)
public class SetBrowserValuesAspect {

    @Autowired
    private BrowserValuesProvider browserValuesProvider;

    @Before(value = "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void setValues(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0 && args[0] instanceof AuthenticatedRequestBean) {
            browserValuesProvider.setTimezoneOffset((AuthenticatedRequestBean)args[0]);
        }
        DateUtils.setTimezoneProvider(browserValuesProvider);
    }

}
