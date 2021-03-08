package org.commcare.formplayer.tests;

import org.commcare.cases.model.Case;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Regression tests for submission behaviors
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class SubmitTests extends BaseTestClass {
    @Test
    public void testCaseCreateFails() throws Exception {
        // Start new session and submit create case form
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_3.json",
                "xforms/cases/create_case.xml");
        String sessionId = newSessionResponse.getSessionId();

        UserSqlSandbox sandbox = getRestoreSandbox();
        SqlStorage<Case> caseStorage =  sandbox.getCaseStorage();
        assertThat(caseStorage.getNumRecords()).isEqualTo(15);

        Mockito.doThrow(HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "", new HttpHeaders(), new byte[0], null)
        ).when(submitServiceMock).submitForm(anyString(), anyString());
        // Assert that FormSession is not deleted
        Mockito.doThrow(new RuntimeException())
                .when(formSessionService).deleteSessionById(anyString());

        SubmitResponseBean submitResponseBean = submitForm("requests/submit/submit_request_case.json", sessionId);
        assert submitResponseBean.getStatus().equals("error");
        // Assert that case is not created
        assert(caseStorage.getNumRecords()== 15);

        sandbox.getConnection().close();
    }

}
