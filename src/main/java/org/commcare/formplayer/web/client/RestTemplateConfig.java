package org.commcare.formplayer.web.client;

import org.commcare.formplayer.util.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.URISyntaxException;
import java.time.Duration;

import lombok.extern.apachecommons.CommonsLog;

@Configuration
@CommonsLog
public class RestTemplateConfig {

    public static String MODE_REPLACE_HOST = "replace-host";

    @Value("${formplayer.externalRequestMode}")
    private String externalRequestMode;

    @Value("${commcarehq.host}")
    private String commcareHost;

    public RestTemplateConfig() {
    }

    /**
     * Constructor for tests
     */
    public RestTemplateConfig(String commcareHost, String externalRequestMode) {
        this.commcareHost = commcareHost;
        this.externalRequestMode = externalRequestMode;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) throws URISyntaxException {
        builder = builder
                .setConnectTimeout(Duration.ofMillis(Constants.CONNECT_TIMEOUT))
                .setReadTimeout(Duration.ofMillis(Constants.READ_TIMEOUT))
                .requestFactory(OkHttp3ClientHttpRequestFactory.class);

        if (externalRequestMode.equals(MODE_REPLACE_HOST)) {
            log.warn(String.format("RestTemplate configured in '%s' mode", externalRequestMode));
            builder = builder.additionalInterceptors(
                    new RewriteHostRequestInterceptor(commcareHost));
        }
        return builder.build();
    }
}
