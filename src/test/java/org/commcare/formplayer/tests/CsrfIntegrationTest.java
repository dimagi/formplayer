package org.commcare.formplayer.tests;

import org.commcare.formplayer.configuration.WebSecurityConfig;
import org.commcare.formplayer.services.HqUserDetailsService;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
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

/**
 * @author $|-|!˅@M
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, FilterChainProxy.class, HqUserDetailsService.class, WebSecurityConfig.class })
public class CsrfIntegrationTest extends BaseTestClass {
    @Value("${commcarehq.host}")
    private String host;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private RestTemplate restTemplate;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
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
}
