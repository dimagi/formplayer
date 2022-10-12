package org.commcare.formplayer.web.client;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

class RestTemplateConfigTest_noCustomization {

    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void init() throws URISyntaxException {
        restTemplate = getRestTemplate("https://web", "");
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    protected RestTemplate getRestTemplate(String commcareHost, String mode)
            throws URISyntaxException {
        return new RestTemplateConfig(commcareHost, mode).defaultRestTemplate(new RestTemplateBuilder());
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
    public RestTemplate getRestTemplate(String commcareHost, String mode)
            throws URISyntaxException {
        return super.getRestTemplate(commcareHost, RestTemplateConfig.MODE_REPLACE_HOST);
    }

    @Override
    protected String getExpectedUrl() {
        return "https://web/a/demo/receiver/1234";
    }
}
