package org.commcare.formplayer.util;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.exceptions.ApplicationConfigException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class NotificationLogger {

    @Autowired
    private FormplayerDatadog datadog;

    public void logNotification(@Nullable NotificationMessage notification, HttpServletRequest req) {
        if (notification == null) {
            return;
        }
        try {
            if (notification.getType().equals(NotificationMessage.Type.error.name())) {
                Sentry.captureException(new RuntimeException(notification.getMessage()));
                datadog.incrementErrorCounter(Constants.DATADOG_ERRORS_NOTIFICATIONS, req, notification.getTag());
            } else if (notification.getType().equals(NotificationMessage.Type.app_error.name())) {
                FormplayerSentry.captureException(new ApplicationConfigException(notification.getMessage()), SentryLevel.INFO);
                datadog.incrementErrorCounter(Constants.DATADOG_ERRORS_APP_CONFIG, req, notification.getTag());
            }
        } catch (Exception e) {
            // we don't wanna crash while logging the error
        }
    }
}
