package org.commcare.formplayer.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;
import io.sentry.dsn.InvalidDsnException;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;

/**
 * Provides stateless reporting to Sentry for individual events that fall outside of the clean
 * request cycle.
 *
 * @author Clayton Sims <csims@dimagi.com>
 *
 */
@Service
public class FallbackSentryReporter {
    @Value("${sentry.dsn:}")
    private String ravenDsn;

    @Value("${commcarehq.environment}")
    private String environment;

    public EventBuilder getEventForException(Exception e) {
        return new EventBuilder()
            .withEnvironment(environment)
            .withMessage(e.getMessage())
            .withLevel(Event.Level.ERROR)
            .withSentryInterface(new ExceptionInterface(e));
    }


    public void sendEvent(EventBuilder event) {
        //TODO: In theory this could stay a singleton and not need to be re-inititalied
        SentryClient client;
        try {
            client = SentryClientFactory.sentryClient(ravenDsn);
            if (client == null) {
                return;
            }
        } catch (InvalidDsnException e) {
            return;
        }

        client.sendEvent(event);
        client.closeConnection();
    }

}
