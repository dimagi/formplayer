package org.commcare.formplayer.web.client;

import com.google.common.collect.ImmutableListMultimap;

import org.commcare.formplayer.services.RestoreFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URISyntaxException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

public class WebClientTest {

    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private WebClient webClient;

    @Mock
    private RestoreFactory restoreFactory;

    @BeforeEach
    public void init() throws URISyntaxException {
        MockitoAnnotations.openMocks(this);

        RestTemplateConfig config = new RestTemplateConfig("", "");
        restTemplate = config.restTemplate(new RestTemplateBuilder());
        mockServer = MockRestServiceServer.createServer(restTemplate);

        webClient = new WebClient();
        webClient.setRestoreFactory(restoreFactory);
        webClient.setRestTemplate(restTemplate);

        when(restoreFactory.getRequestHeaders(any())).thenReturn(new HttpHeaders());
    }

    @Test
    public void testPostFormData() {
        String url = "http://localhost:8000/a/demo/receiver/1234";
        ImmutableListMultimap<String, String> postData = ImmutableListMultimap.of(
                "a", "1",
                "b", "2",
                "b", "not 2"
        );

        MultiValueMap<String, String> expectedBody = new LinkedMultiValueMap<>();
        postData.forEach(expectedBody::add);

        mockServer.expect(ExpectedCount.once(), requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(expectedBody))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.TEXT_HTML)
                        .body("response123")
                );

        // call method under test
        String response = webClient.postFormData(url, postData);
        Assertions.assertEquals("response123", response);

        mockServer.verify();
    }

}
