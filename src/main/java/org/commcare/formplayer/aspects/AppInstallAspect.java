package org.commcare.formplayer.aspects;

import datadog.trace.api.Trace;
import org.commcare.formplayer.beans.InstallRequestBean;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.util.FormplayerSentry;

import java.util.Arrays;

/**
 * Aspect to configure the FormplayerStorageManager
 */
@Aspect
@Order(6)
public class AppInstallAspect {

    @Autowired
    protected FormplayerStorageFactory storageFactory;

    @Autowired
    private FormplayerSentry raven;

    @Before(value = "@annotation(org.commcare.formplayer.annotations.AppInstall)")
    @Trace
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
                        "domain", requestBean.getDomain(),
                        "locale", requestBean.getLocale()
                )
                .setCategory("app_info")
                .record();
        raven.setAppId(requestBean.getAppId());
    }
}
