package org.commcare.formplayer.tests;

import org.commcare.formplayer.application.RestTemplateConfig;
import org.commcare.formplayer.repo.FormSessionRepo;
import org.commcare.formplayer.services.CategoryTimingHelper;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.services.SubmitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;

@RestClientTest(components = {SubmitService.class, RestTemplateConfig.class})
public class SubmitServiceTests {

    // mock this so we don't need to configure a DB
    @MockBean
    FormSessionRepo formSessionRepo;

    @MockBean
    RestoreFactory restoreFactory;

    @MockBean
    CategoryTimingHelper categoryTimingHelper;

    @Autowired
    private SubmitService service;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    RestTemplate errorPassthroughRestTemplate;

    //SpringRunner doesn't know about requestscopes by default
    @TestConfiguration
    static class RequestScopeConfig {
        @Bean
        public BeanFactoryPostProcessor injectRequestScope(){
            return beanFactory -> beanFactory.registerScope(WebApplicationContext.SCOPE_REQUEST, new SimpleThreadScope());
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        this.server.reset();
        server= MockRestServiceServer.createServer(errorPassthroughRestTemplate);
    }

    @Test
    public void assertThatErrorsPassThrough() {
        when(restoreFactory.getDomain()).thenReturn("arealdomain");

        CategoryTimingHelper.RecordingTimer mockedTimer = Mockito.mock(CategoryTimingHelper.RecordingTimer.class);
        when(mockedTimer.end()).thenReturn(mockedTimer);
        when(categoryTimingHelper.newTimer(anyString(), anyString())).thenReturn(mockedTimer);

        String serverUrl = "https://formplayer.test/receiver";

        this.server.expect(requestTo(serverUrl)).andRespond(withBadRequest());

        assertThat(this.service.submitForm("error", serverUrl).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
