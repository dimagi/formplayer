package org.commcare.formplayer.tests;

import org.commcare.cases.model.Case;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Regression tests for submission behaviors
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class SubmitTests extends BaseTestClass {

    @Test
    public void testLocalCaseCreateFailsWhenSubmissionToRemoteFails() throws Exception {
        String sessionId = startSession();

        SubmitResponseBean submitResponseBean = submitWithHttpError(sessionId);
        assertEquals("error", submitResponseBean.getStatus());

        // Assert that case is not created
        assertLocalCaseCount(15);

        // Check that the session has not been deleted
        Assertions.assertNotNull(formSessionService.getSessionById(sessionId));
    }

    @Test
    public void testSubmissionSuccessfulAfterProcessingFailure() throws Exception {
        String sessionId = startSession();
        SubmitResponseBean submitResponseBean = submitWithHttpError(sessionId);
        assertEquals("error", submitResponseBean.getStatus());

        SubmitResponseBean response = submitForm("requests/submit/submit_request_case.json", sessionId);
        assertEquals("success", response.getStatus());
        Assertions.assertThrows(FormNotFoundException.class, () -> formSessionService.getSessionById(sessionId));
    }

    private String startSession() throws Exception {
        // Start new session and submit create case form
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_3.json",
                "xforms/cases/create_case.xml");
        String sessionId = newSessionResponse.getSessionId();
        assertLocalCaseCount(15);
        return sessionId;
    }

    private SubmitResponseBean submitWithHttpError(String sessionId) throws Exception {
        Mockito.doThrow(HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "", new HttpHeaders(), new byte[0], null)
        ).when(submitServiceMock).submitForm(anyString(), anyString());

        return submitForm("requests/submit/submit_request_case.json", sessionId);
    }

    private void assertLocalCaseCount(int expected) {
        UserSqlSandbox sandbox = getRestoreSandbox();
        SqlStorage<Case> caseStorage =  sandbox.getCaseStorage();
        assertThat(caseStorage.getNumRecords()).isEqualTo(expected);
    }

}
