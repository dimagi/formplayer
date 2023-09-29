package org.commcare.formplayer.application;

import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.util.NotificationLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class NotificationHelper {

    @Autowired
    private NotificationLogger notificationLogger;

    void logNotification(@Nullable NotificationMessage notification, HttpServletRequest req) {
        notificationLogger.logNotification(notification, req);
    }
}
