package org.commcare.formplayer.web.client;

import org.commcare.formplayer.utils.MockRestTemplateBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(MockitoExtension.class)
class RestTemplateConfigTest_noCustomization {

    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void init() throws URISyntaxException {
        restTemplate = getRestTemplate("https://web");
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    protected RestTemplate getRestTemplate(String commcareHost)
            throws URISyntaxException {
        return new MockRestTemplateBuilder().withCommcareHost(commcareHost).getRestTemplate();
    }

    protected String getExpectedUrl() {
        return "http://localhost:8000/a/demo/receiver/1234";
    }

    @Test
    public void testRestTemplate() throws URISyntaxException {
        mockServer.expect(ExpectedCount.once(), requestTo(new URI(getExpectedUrl())))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.TEXT_HTML)
                        .body("response")
                );

        String url = "http://localhost:8000/a/demo/receiver/1234";
        restTemplate.getForObject(url, String.class);
        mockServer.verify();
    }
}


class RestTemplateConfigTest_replaceHost extends RestTemplateConfigTest_noCustomization {
    @Override
    public RestTemplate getRestTemplate(String commcareHost)
            throws URISyntaxException {
        return new MockRestTemplateBuilder()
                .withCommcareHost(commcareHost)
                .withExternalRequestMode(RestTemplateConfig.MODE_REPLACE_HOST)
                .getRestTemplate();
    }

    @Override
    protected String getExpectedUrl() {
        return "https://web/a/demo/receiver/1234";
    }
}
