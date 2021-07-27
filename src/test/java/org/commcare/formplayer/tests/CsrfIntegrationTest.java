package org.commcare.formplayer.tests;

import org.commcare.formplayer.application.UtilController;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.utils.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CsrfIntegrationTest {
    @Value("${commcarehq.host}")
    private String host;

    @LocalServerPort
    private int port;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    TestRestTemplate testRestTemplate;

    protected MockMvc mockUtilController;

    @InjectMocks
    protected UtilController utilController;


    @BeforeEach
    public void setUp() throws Exception {
        mockUserDetailResponse();
        mockUtilController = MockMvcBuilders
                .standaloneSetup(utilController)
                .apply(springSecurity(springSecurityFilterChain))
                .build();
    }

    private void mockUserDetailResponse() {
        String detailsString = "{" +
                "\"domains\":[\"casetestdomain\"]," +
                "\"djangoUserId\":1," +
                "\"username\":\"casetestuser\"," +
                "\"superUser\":false" +
                "}";
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(requestTo(host + Constants.SESSION_DETAILS_VIEW))
                .andExpect(jsonPath("$.sessionId").value("derp"))
                .andExpect(header("X-MAC-DIGEST", "fOvC0ttB/nSI4CkRc5quDvNlDBuA2aXGZWgZLgMorbg="))
                .andExpect(jsonPath("$.domain").value("casetestdomain"))
                .andRespond(withSuccess(detailsString, MediaType.APPLICATION_JSON));
    }

    @Test
    public void postApiCall_withCsrf_succeeds() throws Exception {
        String payload = FileUtils.getFile(this.getClass(), "requests/delete_db/delete_db.json");
        mockUtilController.perform(
            post("/" + Constants.URL_DELETE_APPLICATION_DBS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
                    .with(testUser())
                    .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                    .with(csrf())
        ).andExpect(status().isOk());
    }

    private RequestPostProcessor testUser() {
        return user("user1").password("user1Pass").roles("USER");
    }

    @Test
    public void postApiCall_withoutCsrf_fails() throws Exception {
        String payload = FileUtils.getFile(this.getClass(), "requests/delete_db/delete_db.json");
        mockUtilController.perform(
                post("/" + Constants.URL_DELETE_APPLICATION_DBS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                        .content(payload)
        ).andExpect(status().isForbidden());
    }

    @Test
    public void getApiCall_withoutCsrf_succeeds() throws Exception {
        mockUtilController.perform(
                get("/" + Constants.URL_SERVER_UP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
        ).andExpect(status().isOk());
    }

    @Test
    public void postApiCall_withHmacHeader_withoutCsrf_succeeds() throws Exception {
        String payload = FileUtils.getFile(this.getClass(), "requests/delete_db/delete_db.json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Cookie", Constants.POSTGRES_DJANGO_SESSION_ID + "=" + "derp");
        headers.add(Constants.HMAC_HEADER, "BHOwo3mPXbtWM91RO0g5HQOt+DtiiQVnCWMFsvjkWVc=");
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = testRestTemplate.exchange(
                "http://localhost:" + port + "/" + Constants.URL_DELETE_APPLICATION_DBS,
                HttpMethod.POST, entity, String.class);

        assert response.getStatusCode() == HttpStatus.OK;
    }
}
