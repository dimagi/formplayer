package aspects;

import beans.InstallFromSessionRequestBean;
import beans.InstallRequestBean;
import com.getsentry.raven.event.BreadcrumbBuilder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import services.FormplayerStorageFactory;
import util.FormplayerRaven;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
        InstallRequestBean requestBean = (InstallRequestBean) args[0];
        storageFactory.configure(requestBean);

        Map<String, String> data = new HashMap<String, String>();
        data.put("appId", requestBean.getAppId());
        data.put("installReference", requestBean.getInstallReference());
        data.put("locale", requestBean.getLocale());

        BreadcrumbBuilder builder = new BreadcrumbBuilder();
        builder.setData(data);
        builder.setCategory("application_install");
        raven.recordBreadcrumb(builder.build());
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
