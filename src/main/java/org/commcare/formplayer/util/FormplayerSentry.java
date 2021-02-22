package org.commcare.formplayer.util;

import io.sentry.Breadcrumb;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.commcare.formplayer.services.RestoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.util.HashMap;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class FormplayerSentry {

    private static final Log log = LogFactory.getLog(FormplayerSentry.class);

    private final String HQ_HOST_TAG = "HQHost";
    private final String AS_USER = "as_user";
    private final String URI = "uri";
    private final String APP_URL_EXTRA = "app_url";
    private final String APP_DOWNLOAD_URL_EXTRA = "app_download";
    private final String USER_SYNC_TOKEN = "sync_token";
    private final String USER_SANDBOX_PATH = "sandbox_path";

    private String appId = "UNKNOWN";

    @Value("${commcarehq.host}")
    private String host;

    @Autowired
    private RestoreFactory restoreFactory;

    public static class BreadcrumbRecorder {
        Breadcrumb breadcrumb;

        public BreadcrumbRecorder() {
            this.breadcrumb = new Breadcrumb();
        }

        public BreadcrumbRecorder setData(String... dataPairs) {
            // ignores last element if odd number of elements
            int length = dataPairs.length / 2;
            for (int i = 0; i < length; i++) {
                String key = dataPairs[2 * i];
                String value = dataPairs[2 * i + 1];
                if (key != null && value != null){
                    breadcrumb.setData(key, value);
                }
            }
            return this;
        }

        public BreadcrumbRecorder setType(String type) {
            breadcrumb.setType(type);
            return this;
        }

        public BreadcrumbRecorder setLevel(SentryLevel level) {
            breadcrumb.setLevel(level);
            return this;
        }

        public BreadcrumbRecorder setMessage(String message) {
            breadcrumb.setMessage(message);
            return this;
        }

        public BreadcrumbRecorder setCategory(String category) {
            breadcrumb.setCategory(category);
            return this;
        }

        public Breadcrumb value() {
            return breadcrumb;
        }

        public void record() {
            Sentry.addBreadcrumb(breadcrumb);
        }
    }
    public BreadcrumbRecorder newBreadcrumb() {
        return new BreadcrumbRecorder();
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

    private void configureScope(FormplayerHttpRequest request) {
        Sentry.configureScope(scope -> {
            String username = "unknown";
            String synctoken = "unknown";
            String sandboxPath = "unknown";
            if (restoreFactory != null && restoreFactory.isConfigured()) {
                username = restoreFactory.getEffectiveUsername();
                synctoken = restoreFactory.getSyncToken();
                sandboxPath = restoreFactory.getSQLiteDB().getDatabaseFileForDebugPurposes();
            }

            scope.setTag(HQ_HOST_TAG, host);

            scope.setTag(AS_USER, username);
            scope.setExtra(USER_SYNC_TOKEN, synctoken);
            scope.setExtra(USER_SANDBOX_PATH, sandboxPath);

            if (request != null) {
                scope.setTag(URI, request.getRequestURI());

                String domain = request.getDomain();
                if (domain != null) {
                    scope.setTag(Constants.DOMAIN_TAG, domain);
                    if (appId != null) {
                        scope.setExtra(APP_DOWNLOAD_URL_EXTRA, getAppDownloadURL(domain, appId));
                        scope.setExtra(APP_URL_EXTRA, getAppURL(domain, appId));
                    }
                }
            }
        });
    }
    public void sendRavenException(Exception exception) {
        sendRavenException(exception, SentryLevel.ERROR);
    }

    public void sendRavenException(Exception exception, SentryLevel level) {
        if (!Sentry.isEnabled()) {
            return;
        }

        Sentry.withScope(scope -> {
            FormplayerHttpRequest request = RequestUtils.getCurrentRequest();
            configureScope(request);

            SentryEvent event = new SentryEvent();
            Message message = new Message();
            message.setMessage(exception.getMessage());
            event.setMessage(message);
            event.setLevel(level);
            event.setThrowable(exception);
            Sentry.captureEvent(event);
        });
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }
}
