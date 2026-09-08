package org.commcare.formplayer.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.commcare.formplayer.auth.PublicSessionCredential;
import org.commcare.formplayer.auth.UserDomainPreAuthPrincipal;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.exceptions.SessionAuthUnavailableException;
import org.commcare.formplayer.repo.FormDefinitionRepo;
import org.commcare.formplayer.repo.FormSessionRepo;
import org.commcare.formplayer.repo.MediaMetaDataRepo;
import org.commcare.formplayer.repo.MenuSessionRepo;
import org.commcare.formplayer.repo.VirtualDataInstanceRepo;
import org.commcare.formplayer.services.HqUserDetailsService;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.web.client.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;

@SpringBootTest
@AutoConfigureMockRestServiceServer
@TestPropertySource(properties = {
        "commcarehq.host=",
        "commcarehq.formplayerAuthKey=secretkey"
})
public class HqUserDetailsServiceTests {

    // mock this so we don't need to configure a DB
    @MockBean
    public FormSessionRepo formSessionRepo;

    // mock this so we don't need to configure a DB
    @MockBean
    public FormDefinitionRepo formDefinitionRepo;

    // mock this so we don't need to configure a DB
    @MockBean
    public MenuSessionRepo menuSessionRepo;

    // mock this so we don't need to configure a DB
    @MockBean
    public VirtualDataInstanceRepo virtualDataInstanceRepo;

    // mock this so we don't need to configure a DB
    @MockBean
    public MediaMetaDataRepo mediaMetaDataRepo;

    @MockBean
    public RestoreFactory RestoreFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HqUserDetailsService service;

    @Autowired
    private MockRestServiceServer server;

    @BeforeEach
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

    @Test
    public void noSession() {
        this.server.expect(requestTo(Constants.SESSION_DETAILS_VIEW))
                .andExpect(jsonPath("$.sessionId").value("invalid"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(SessionAuthUnavailableException.class, () -> {
            this.service.getUserDetails("domain", "invalid");
        });
    }

    @Test
    public void whenCallingGetPublicUserDetails_thenClientSendsPublicSessionKey()
            throws Exception {
        String detailsString = "{" +
                "\"domains\":[\"domain\"]," +
                "\"djangoUserId\":null," +
                "\"username\":\"public@domain\"," +
                "\"authToken\":\"pub-key\"," +
                "\"superUser\":false," +
                "\"public\":true" +
                "}";

        this.server.expect(requestTo(Constants.SESSION_DETAILS_VIEW))
                .andExpect(jsonPath("$.publicSessionKey").value("pub-key"))
                .andExpect(jsonPath("$.sessionId").doesNotExist())
                .andExpect(jsonPath("$.domain").value("domain"))
                .andRespond(withSuccess(detailsString, MediaType.APPLICATION_JSON));

        HqUserDetailsBean details = this.service.getPublicUserDetails("domain", "pub-key");

        assertThat(details.getUsername()).isEqualTo("public@domain");
        assertThat(details.getDomains()).isEqualTo(new String[]{"domain"});
        assertThat(details.isPublicSession()).isTrue();
    }

    @Test
    public void loadUserDetails_withPublicCredential_routesToPublicSessionKey()
            throws Exception {
        String detailsString = "{" +
                "\"domains\":[\"domain\"]," +
                "\"djangoUserId\":null," +
                "\"username\":\"citrus\"," +
                "\"authToken\":\"pub-key\"," +
                "\"superUser\":false," +
                "\"public\":true" +
                "}";

        this.server.expect(requestTo(Constants.SESSION_DETAILS_VIEW))
                .andExpect(jsonPath("$.publicSessionKey").value("pub-key"))
                .andExpect(jsonPath("$.sessionId").doesNotExist())
                .andRespond(withSuccess(detailsString, MediaType.APPLICATION_JSON));

        UserDomainPreAuthPrincipal principal = new UserDomainPreAuthPrincipal("citrus", "domain");
        PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(
                principal, new PublicSessionCredential("pub-key"));

        UserDetails details = this.service.loadUserDetails(token);
        assertThat(details.getUsername()).isEqualTo("citrus");
    }

    @Test
    public void loadUserDetails_publicSession_authorizesOnDomainIgnoringClaimedUsername()
            throws Exception {
        String detailsString = "{" +
                "\"domains\":[\"domain\"]," +
                "\"username\":\"public_abc123@domain\"," +
                "\"authToken\":\"pub-key\"," +
                "\"superUser\":false," +
                "\"public\":true" +
                "}";

        this.server.expect(requestTo(Constants.SESSION_DETAILS_VIEW))
                .andRespond(withSuccess(detailsString, MediaType.APPLICATION_JSON));

        UserDomainPreAuthPrincipal principal = new UserDomainPreAuthPrincipal("someone-else", "domain");
        PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(
                principal, new PublicSessionCredential("pub-key"));

        UserDetails details = this.service.loadUserDetails(token);
        assertThat(details.getUsername()).isEqualTo("public_abc123@domain");
    }
}
