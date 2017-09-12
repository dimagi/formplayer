package aspects;

import beans.InstallFromSessionRequestBean;
import beans.InstallRequestBean;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import services.FormplayerStorageFactory;
import util.FormplayerRaven;

import java.util.Arrays;

/**
 * Aspect to configure the FormplayerStorageManager
 */
@Aspect
@Order(5)
public class AppInstallAspect {

    @Autowired
    protected FormplayerStorageFactory storageFactory;

    @Autowired
    private FormplayerRaven raven;

    @Before(value = "@annotation(annotations.AppInstall)")
    public void configureStorageFactory(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (!(args[0] instanceof InstallRequestBean)) {
            throw new RuntimeException("Could not configure StorageFactory with args " + Arrays.toString(args));
        }
        final InstallRequestBean requestBean = (InstallRequestBean) args[0];
        storageFactory.configure(requestBean);

        raven.newBreadcrumb()
                .setData(
                        "appId", requestBean.getAppId(),
                        "installReference", requestBean.getInstallReference(),
                        "locale", requestBean.getLocale()
                )
                .setCategory("application_install")
                .record();
        raven.setAppId(requestBean.getAppId());
    }

    @Before(value = "@annotation(annotations.AppInstallFromSession)")
    public void configureStorageFactoryFromSession(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (!(args[0] instanceof InstallFromSessionRequestBean)) {
            throw new RuntimeException("Could not configure StorageFactory with args " + Arrays.toString(args));
        }
        InstallFromSessionRequestBean requestBean = (InstallFromSessionRequestBean) args[0];
        storageFactory.configure(requestBean.getMenuSessionId());
    }

}
