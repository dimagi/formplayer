package org.commcare.formplayer.web.client;

import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.RequestUtils;
import org.commcare.formplayer.utils.HqUserDetails;
import org.commcare.formplayer.utils.WithHqUserSecurityContextFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(MockitoExtension.class)
class RestTemplateAuthTest {

    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @Mock
    private ServletRequestAttributes requestAttributes;

    @Mock
    private HttpServletRequest request;
    private String commcareHost = "https://www.commcarehq.org";
    private String formplayerAuthKey = "authKey";

    @BeforeEach
    public void init() throws URISyntaxException {
        restTemplate = new RestTemplateConfig(commcareHost, formplayerAuthKey, "")
                .restTemplate(new RestTemplateBuilder());
        mockServer = MockRestServiceServer.createServer(restTemplate);
        RequestContextHolder.setRequestAttributes(requestAttributes);
        when(requestAttributes.getRequest()).thenReturn(request);
        WithHqUserSecurityContextFactory.setSecurityContext(
                HqUserDetails.builder().username("testUser").authToken("123abc").build()
        );
    }

    private void mockHmacRequest() {
        when(request.getAttribute(eq(Constants.HMAC_REQUEST_ATTRIBUTE))).thenReturn(true);
    }

    @Test
    public void testRestTemplateSessionAuth() throws URISyntaxException {
        String url = commcareHost + "/a/demo/receiver/1234";

        expectRequest(url, HttpMethod.GET)
                .andExpect(sessionAuth("123abc"))
                .andRespond(response());

        restTemplate.getForObject(url, String.class);
        mockServer.verify();
    }

    @Test
    public void testRestTemplateSessionAuth_notForCommCare() throws URISyntaxException {
        String url = "https://www.otherhost.com/a/demo/receiver/1234";
        expectRequest(url, HttpMethod.GET)
                .andExpect(noAuth())
                .andRespond(response());

        restTemplate.getForObject(url, String.class);
        mockServer.verify();
    }

    @Test
    public void testRestTemplateHmacAuth_GET() throws Exception {
        mockHmacRequest();

        String url = "/a/demo/receiver/1234?a=1&b=2&as=testUser";
        String authHeader = RequestUtils.getHmac(formplayerAuthKey, url.getBytes(StandardCharsets.UTF_8));

        String fullUrl = commcareHost + url;
        expectRequest(fullUrl, HttpMethod.GET)
                .andExpect(hmacAuth(authHeader))
                .andRespond(response());

        restTemplate.getForObject(fullUrl, String.class);
        mockServer.verify();
    }

    @Test
    public void testRestTemplateHmacAuth_POST() throws Exception {
        mockHmacRequest();

        String body = "This is the POST body";
        String authHeader = RequestUtils.getHmac(formplayerAuthKey, body.getBytes(StandardCharsets.UTF_8));

        String url = commcareHost + "/a/demo/receiver/1234?a=1&b=2&as=testUser";
        expectRequest(url, HttpMethod.POST)
                .andExpect(hmacAuth(authHeader))
                .andRespond(response());

        restTemplate.postForObject(url, body, String.class);
        mockServer.verify();
    }

    @Test
    public void testRestTemplateHmacAuth_addsAsParam() throws Exception {
        mockHmacRequest();

        String url = "/a/demo/receiver/1234?a=1&b=2";
        String expectedUrl = url + "&as=testUser";
        String authHeader = RequestUtils.getHmac(formplayerAuthKey, expectedUrl.getBytes(StandardCharsets.UTF_8));

        expectRequest(commcareHost + expectedUrl, HttpMethod.GET)
                .andExpect(hmacAuth(authHeader))
                .andRespond(response());

        restTemplate.getForObject(commcareHost + url, String.class);
        mockServer.verify();
    }

    @Test
    public void testRestTemplateHmacAuth_notCommCare() throws Exception {
        mockHmacRequest();

        String url = "http://localhost/a/demo/receiver/1234?a=1&b=2";
        expectRequest(url, HttpMethod.GET)
                .andExpect(noAuth())
                .andRespond(response());

        restTemplate.getForObject(url, String.class);
        mockServer.verify();
    }

    private ResponseActions expectRequest(String url, HttpMethod method) throws URISyntaxException {
        return mockServer.expect(ExpectedCount.once(), requestTo(new URI(url)))
                .andExpect(method(method));
    }

    private RequestMatcher hmacAuth(String authHeader) {
        return compoundMatcher(
                header(Constants.HMAC_HEADER, authHeader),
                headerDoesNotExist(Constants.POSTGRES_DJANGO_SESSION_ID),
                headerDoesNotExist("Authorization"),
                headerDoesNotExist("Cookie")
        );
    }

    private RequestMatcher sessionAuth(String token) {
        String authHeader = Constants.POSTGRES_DJANGO_SESSION_ID + "=" + token;
        return compoundMatcher(
                header(Constants.POSTGRES_DJANGO_SESSION_ID, token),
                header("Cookie", authHeader),
                header("Authorization", authHeader),
                headerDoesNotExist(Constants.HMAC_HEADER)
        );
    }

    private RequestMatcher noAuth() {
        return compoundMatcher(
                headerDoesNotExist(Constants.HMAC_HEADER),
                headerDoesNotExist(Constants.POSTGRES_DJANGO_SESSION_ID),
                headerDoesNotExist("Authorization"),
                headerDoesNotExist("Cookie")
        );
    }

    private ResponseCreator response() {
        return withStatus(HttpStatus.OK)
                .contentType(MediaType.TEXT_HTML)
                .body("response");
    }

    private RequestMatcher compoundMatcher(RequestMatcher... matchers) {
        return request -> {
            for (RequestMatcher matcher : matchers) {
                matcher.match(request);
            }
        };
    }

}
