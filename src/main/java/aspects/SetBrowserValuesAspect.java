package aspects;

import beans.AuthenticatedRequestBean;
import beans.SessionNavigationBean;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.javarosa.core.model.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import services.BrowserValuesProvider;
import services.FormplayerTimezoneSource;

/**
 * Created by amstone326 on 1/8/18.
 */
@Aspect
@Order(1)
public class SetBrowserValuesAspect {

    @Autowired
    private FormplayerTimezoneSource timezoneSource;

    @Autowired
    private BrowserValuesProvider browserValuesProvider;

    @Before(value = "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void setValues(JoinPoint joinPoint) throws Throwable {
        DateUtils.setTimezoneProviderSource(timezoneSource);

        Object beanArg = joinPoint.getArgs()[0];
        if (beanArg instanceof AuthenticatedRequestBean) {
            System.out.println("Setting tz offset in BrowserValuesProvider");
            browserValuesProvider.setTimezoneOffset((AuthenticatedRequestBean)beanArg);
        }
        if (beanArg instanceof SessionNavigationBean) {
            System.out.println("Setting location in BrowserValuesProvider");
            browserValuesProvider.setLocation((SessionNavigationBean)beanArg);
        }
    }

}
