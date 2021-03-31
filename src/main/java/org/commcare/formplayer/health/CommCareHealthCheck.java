package org.commcare.formplayer.health;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Health check to test that we can connect to CommCare
 */
@Component("commcare")
public class CommCareHealthCheck implements HealthIndicator {

    private final HttpClient client;

    @Value("${commcarehq.host}")
    private String host;

    public CommCareHealthCheck() {
        client = HttpClientBuilder.create()
                .setConnectionReuseStrategy(new NoConnectionReuseStrategy())
                .disableRedirectHandling().build();
    }

    @Override
    public Health health() {
        try {
            client.execute(new HttpGet(host));
        } catch (IOException ex) {
            return Health.outOfService().withException(ex).build();
        }
        return Health.up().build();
    }
}
