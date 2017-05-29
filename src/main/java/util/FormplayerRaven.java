package util;

import com.getsentry.raven.Raven;
import com.getsentry.raven.event.Breadcrumb;
import com.getsentry.raven.event.Event;
import com.getsentry.raven.event.EventBuilder;
import com.getsentry.raven.event.interfaces.ExceptionInterface;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Created by benrudolph on 4/27/17.
 */
public class FormplayerRaven {
    /**
     * We require a Raven instance in these Utils to ensure that the caller has instantiated
     * the Raven instance. Raven keeps track of the raven instance internally with
     * Raven.getStoredInstance(). If this call returns null, the Sentry call will fail.
     */

    private static final Log log = LogFactory.getLog(FormplayerRaven.class);

    private final String ENV_TAG = "environment";
    private final String HQ_HOST_TAG = "HQHost";
    private final String DOMAIN_TAG = "domain";

    private Raven raven;

    @Value("${commcarehq.environment}")
    private String environment;

    private String domain = "UNKNOWN";

    @Value("${commcarehq.host}")
    private String host;

    public FormplayerRaven(Raven raven) {
        this.raven = raven;
    }

    public void recordBreadcrumb(Breadcrumb breadcrumb) {
        if (raven == null) {
            return;
        }
        try {
            raven.getContext().recordBreadcrumb(breadcrumb);
        } catch (Exception e) {
            log.info("Error recording breadcrumb. Ensure that raven is configured. ", e);
        }
    }

    private EventBuilder getDefaultBuilder() {
        return (
                new EventBuilder()
                .withTag(ENV_TAG, environment)
                .withTag(HQ_HOST_TAG, host)
                .withTag(DOMAIN_TAG, domain)
        );
    }

    public void sendRavenException(Exception exception) {
        sendRavenException(exception, Event.Level.ERROR);
    }

    public void sendRavenException(Exception exception, Event.Level level) {

        EventBuilder eventBuilder = getDefaultBuilder()
                .withMessage(exception.getMessage())
                .withLevel(level)
                .withSentryInterface(new ExceptionInterface(exception));

        sendRavenEvent(eventBuilder);
    }

    private void sendRavenEvent(EventBuilder event) {
        if (raven == null) {
            return;
        }
        try {
            raven.sendEvent(event);
        } catch (Exception e) {
            log.info("Error sending event to Sentry. Ensure that raven is configured. ", e);
        }
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
