package util;

import com.getsentry.raven.Raven;
import com.getsentry.raven.event.Breadcrumb;
import com.getsentry.raven.event.Breadcrumbs;
import com.getsentry.raven.event.EventBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by benrudolph on 4/27/17.
 */
public class SentryUtils {

    private static final Log log = LogFactory.getLog(SentryUtils.class);

    public static void recordBreadcrumb(Breadcrumb breadcrumb) {
        try {
            Breadcrumbs.record(breadcrumb);
        } catch (Exception e) {
            log.info("Error recording breadcrumb. Ensure that raven is configured. " + e.toString());
        }
    }

    public static void sendRavenException(Exception exception) {
        try {
            Raven.getStoredInstance().sendException(exception);
        } catch (Exception e) {
            log.info("Error sending to Sentry. Ensure that raven is configured. " + e.toString());
        }
    }

    public static void sendRavenEvent(EventBuilder event) {
        try {
            Raven.getStoredInstance().sendEvent(event);
        } catch (Exception e) {
            log.info("Error sending to Sentry. Ensure that raven is configured. " + e.toString());
        }
    }
}
