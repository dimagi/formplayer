package org.commcare.formplayer.utils;

import org.commcare.formplayer.web.client.CommCareDefaultHeaders;
import org.commcare.formplayer.web.client.RestTemplateConfig;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestTemplate;

import java.net.URISyntaxException;

public class MockRestTemplateBuilder {

    private String commcareHost = "";
    private String formpayerAuthKey = "";
    private String externalRequestMode = "";

    private ValueOperations originTokens = Mockito.mock(ValueOperations.class);

    public MockRestTemplateBuilder withCommcareHost(String commcareHost) {
        this.commcareHost = commcareHost;
        return this;
    }

    public MockRestTemplateBuilder withFormpayerAuthKey(String formpayerAuthKey) {
        this.formpayerAuthKey = formpayerAuthKey;
        return this;
    }

    public MockRestTemplateBuilder withExternalRequestMode(String externalRequestMode) {
        this.externalRequestMode = externalRequestMode;
        return this;
    }

    public RestTemplate getRestTemplate() throws URISyntaxException {
        RestTemplateConfig config = new RestTemplateConfig(commcareHost, formpayerAuthKey, externalRequestMode);
        CommCareDefaultHeaders commCareDefaultHeaders = new CommCareDefaultHeaders(commcareHost);
        commCareDefaultHeaders.setOriginTokens(originTokens);
        config.setCommCareDefaultHeaders(commCareDefaultHeaders);
        return config.restTemplate(new RestTemplateBuilder());
    }
}
