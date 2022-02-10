package org.commcare.formplayer.aspects;

import org.apache.http.client.utils.URIBuilder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.commcare.formplayer.beans.InstallRequestBean;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerSentry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;

import java.net.URISyntaxException;
import java.util.Arrays;

import datadog.trace.api.Trace;
import io.sentry.Sentry;
import lombok.extern.java.Log;

/**
 * Aspect to configure the FormplayerStorageManager
 */
@Aspect
@Order(6)
@Log
public class AppInstallAspect {

    @Autowired
    protected FormplayerStorageFactory storageFactory;

    @Value("${commcarehq.host}")
    private String host;

    @Before(value = "@annotation(org.commcare.formplayer.annotations.AppInstall)")
    @Trace
    public void configureStorageFactory(JoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (!(args[0] instanceof InstallRequestBean)) {
            throw new RuntimeException(
                    "Could not configure StorageFactory with args " + Arrays.toString(args));
        }
        final InstallRequestBean requestBean = (InstallRequestBean)args[0];
        storageFactory.configure(requestBean);

        FormplayerSentry.newBreadcrumb()
                .setData(
                        "appId", requestBean.getAppId(),
                        "domain", requestBean.getDomain(),
                        "locale", requestBean.getLocale()
                )
                .setCategory("app_info")
                .record();

        configureAppUrls(requestBean.getDomain(), requestBean.getAppId());
    }

    public void configureAppUrls(String domain, String appId) {
        Sentry.configureScope(scope -> {
            scope.setExtra(Constants.APP_DOWNLOAD_URL_EXTRA, getAppDownloadURL(domain, appId));
            scope.setExtra(Constants.APP_URL_EXTRA, getAppURL(domain, appId));
        });
    }

    private String getAppURL(String domain, String appId) {
        return host + "/a/" + domain + "/apps/view/" + appId + "/";
    }

    private String getAppDownloadURL(String domain, String appId) {
        String baseURL = host + "/a/" + domain + "/apps/api/download_ccz/";
        URIBuilder builder;
        try {
            builder = new URIBuilder(baseURL);
        } catch (URISyntaxException e) {
            log.info("Unable to build app download URL");
            return "unknown";
        }
        builder.addParameter("app_id", appId);
        builder.addParameter("latest", Constants.CCZ_LATEST_SAVED);
        return builder.toString();
    }
}
