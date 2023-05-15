package org.commcare.formplayer.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.commcare.formplayer.objects.SerializableFormSession.SubmitStatus.PROCESSED_XML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;

import org.commcare.cases.model.Case;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

/**
 * Regression tests for submission behaviors
 */
@WebMvcTest
public class SubmitTests extends BaseTestClass {
    static Map<String, Object> answers = ImmutableMap.of("0", "name", "1", "1");

    @BeforeEach
    public void setUpLocal() {
        configureRestoreFactory("basic_eofdomain", "basic_eofusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/basic.xml";
    }

    @Test
    public void testLocalCaseCreateFailsWhenSubmissionToRemoteFails() throws Exception {
        String sessionId = startSession("2", "0");

        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "", new HttpHeaders(), new byte[0], null);
        when(submitServiceMock.submitForm(any(), anyString())).thenThrow(exception);

        SubmitResponseBean submitResponseBean = submitForm(answers, sessionId);

        assertEquals("error", submitResponseBean.getStatus());

        // Assert that case is not created
        assertLocalCaseCount(116);

        // Check that the session has not been deleted
        Assertions.assertNotNull(formSessionService.getSessionById(sessionId));
    }

    @Test
    public void testSubmissionSuccessfulAfterProcessingFailure() throws Exception {
        String sessionId = startSession("2", "0");

        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "", new HttpHeaders(), new byte[0], null);
        when(submitServiceMock.submitForm(any(), anyString()))
                .thenThrow(exception)
                .thenReturn("<OpenRosaResponse>" +
                        "<message nature='status'>" +
                        "OK" +
                        "</message>" +
                        "</OpenRosaResponse>");

        SubmitResponseBean submitResponseBean = submitForm(answers, sessionId);
        assertEquals("error", submitResponseBean.getStatus());

        SubmitResponseBean response = submitForm(answers, sessionId);

        assertEquals("success", response.getStatus());
        Assertions.assertThrows(FormNotFoundException.class,
                () -> formSessionService.getSessionById(sessionId));
    }

    @Test
    public void testSubmissionSuccessfulAfterStackFailure() throws Exception {
        String sessionId = startSession("2", "0");

        doThrow(new Exception("mock stack fail"))
                .doCallRealMethod()
                .when(menuSessionRunnerService).resolveFormGetNext(any());

        SubmitResponseBean errorResponse = submitForm(answers, sessionId);
        assertEquals("error", errorResponse.getStatus());

        SerializableFormSession session = formSessionService.getSessionById(sessionId);
        assertEquals(PROCESSED_XML, session.getSubmitStatus());
        assertLocalCaseCount(117);

        SubmitResponseBean response = submitForm(answers, sessionId);
        assertEquals("success", response.getStatus());
        Assertions.assertThrows(FormNotFoundException.class,
                () -> formSessionService.getSessionById(sessionId));
        assertLocalCaseCount(117);
    }

    private String startSession(String... selections) throws Exception {
        NewFormResponse response = sessionNavigate(selections, "basic", NewFormResponse.class);
        String sessionId = response.getSessionId();
        assertLocalCaseCount(116);
        return sessionId;
    }

    private void assertLocalCaseCount(int expected) {
        UserSqlSandbox sandbox = getRestoreSandbox();
        SqlStorage<Case> caseStorage = sandbox.getCaseStorage();
        assertThat(caseStorage.getNumRecords()).isEqualTo(expected);
    }

}
