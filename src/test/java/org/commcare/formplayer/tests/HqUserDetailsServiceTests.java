package org.commcare.formplayer.tests;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.commcare.formplayer.application.WebAppContext;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.exceptions.SessionAuthUnavailableException;
import org.commcare.formplayer.services.HqUserDetailsService;
import org.commcare.formplayer.util.Constants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@RestClientTest(value=HqUserDetailsService.class)
@AutoConfigureWebClient(registerRestTemplate = true)
@TestPropertySource(properties = {
        "commcarehq.host=",
        "commcarehq.formplayerAuthKey=secretkey"
})
public class HqUserDetailsServiceTests {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HqUserDetailsService service;

    @Autowired
    private MockRestServiceServer server;

    @Before
    public void setUp() throws Exception {
        this.server.reset();
    }

    @Test
    public void whenCallingGetUserDetails_thenClientMakesCorrectCall()
            throws Exception {
        String detailsString = "{" +
                "\"domains\":[\"domain\"]," +
                "\"djangoUserId\":1," +
                "\"username\":\"user@domain.commcarehq.org\"," +
                "\"authToken\":\"authToke\"," +
                "\"superUser\":false" +
                "}";

        this.server.expect(requestTo(Constants.SESSION_DETAILS_VIEW))
                .andExpect(jsonPath("$.sessionId").value("123abc"))
                .andExpect(header("X-MAC-DIGEST", "4mpTOxhuJ+QJQcbeEPtRkr9goVhNh9HP2NszeP+bguc="))
                .andExpect(jsonPath("$.domain").value("domain"))
                .andRespond(withSuccess(detailsString, MediaType.APPLICATION_JSON));

        HqUserDetailsBean details = this.service.getUserDetails("domain", "123abc");

        assertThat(details.getUsername()).isEqualTo("user@domain.commcarehq.org");
        assertThat(details.getDomains()).isEqualTo(new String[]{"domain"});
    }

    @Test(expected = SessionAuthUnavailableException.class)
    public void noSession() {
        this.server.expect(requestTo(Constants.SESSION_DETAILS_VIEW))
                .andExpect(jsonPath("$.sessionId").value("invalid"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        this.service.getUserDetails("domain", "invalid");
    }
}