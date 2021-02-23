package org.commcare.formplayer.util;

import io.sentry.Breadcrumb;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.net.URISyntaxException;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class FormplayerSentry {

    private static final Log log = LogFactory.getLog(FormplayerSentry.class);

    public static final String APP_URL_EXTRA = "app_url";
    public static final String APP_DOWNLOAD_URL_EXTRA = "app_download";

    @Value("${commcarehq.host}")
    private String host;

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

    public void configureAppUrls(@NotNull String domain, @NotNull String appId) {
        Sentry.configureScope(scope -> {
            scope.setExtra(APP_DOWNLOAD_URL_EXTRA, getAppDownloadURL(domain, appId));
            scope.setExtra(APP_URL_EXTRA, getAppURL(domain, appId));
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

    /**
     * Special method to allow capturing exceptions at levels other than ERROR
     */
    public void captureException(Exception exception, SentryLevel level) {
        if (!Sentry.isEnabled()) {
            return;
        }

        SentryEvent event = new SentryEvent();
        Message message = new Message();
        message.setMessage(exception.getMessage());
        event.setMessage(message);
        event.setLevel(level);
        event.setThrowable(exception);
        Sentry.captureEvent(event);
    }
}
