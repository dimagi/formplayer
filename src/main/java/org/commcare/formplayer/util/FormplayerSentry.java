package org.commcare.formplayer.util;

import io.sentry.Breadcrumb;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class FormplayerSentry {

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
