package org.commcare.formplayer.tests;

import org.commcare.formplayer.services.FormattedQuestionsService;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.utils.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Regressions for the formatted questions service
 */
@RunWith(SpringRunner.class)
@RestClientTest({FormattedQuestionsService.class})
@AutoConfigureWebClient(registerRestTemplate = true)
@TestPropertySource(properties = {
        "commcarehq.host=https://test.com"
})
public class FormattedQuestionsServiceTests {

    @MockBean
    RestoreFactory restoreFactory;

    @Autowired
    private FormattedQuestionsService service;

    @Autowired
    private MockRestServiceServer server;

    @Before
    public void setUp() throws Exception {
        this.server.reset();
    }

    @Test
    public void checkFormattedQuestionsServiceResponse() {
        String serverUrl = "https://test.com/a/mydomain/cloudcare/api/readable_questions/";

        this.server.expect(requestTo(serverUrl)).andExpect(method(HttpMethod.POST)).andRespond(
                withSuccess(FileUtils.getFile(this.getClass(), "requests/formatted_questions/base_response.json"), null));

        FormattedQuestionsService.QuestionResponse response = service.getFormattedQuestions("mydomain", "appid", "xmlns", "instancexml");

        assertThat(response.getFormattedQuestions()).isEqualTo("formdatatext");
        assertThat(response.getQuestionList().length()).isEqualTo(2);
        assertThat(response.getQuestionList().get(0)).isEqualTo("one");
        assertThat(response.getQuestionList().get(1)).isEqualTo("two");
    }
}