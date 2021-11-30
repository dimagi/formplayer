package org.commcare.formplayer.tests;

import org.commcare.cases.model.Case;
import org.commcare.cases.util.CasePurgeFilter;
import org.commcare.formplayer.beans.EvaluateXPathResponseBean;
import org.commcare.formplayer.beans.FormEntryResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.QuestionBean;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by willpride on 1/14/16.
 *
 * This test tests that we can create and delete a case via the form API
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class CaseTests extends BaseTestClass {

    @Test
    public void testCaseCreate() throws Exception {
        // Start new session and submit create case form
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_3.json",
                "xforms/cases/create_case.xml");

        UserSqlSandbox sandbox = getRestoreSandbox();

        SqlStorage<Case> caseStorage =  sandbox.getCaseStorage();

        assertEquals(15, caseStorage.getNumRecords());
        sandbox.getConnection().close();

        String sessionId = newSessionResponse.getSessionId();

        answerQuestionGetResult("0", "Tom Brady", sessionId);
        answerQuestionGetResult("1", "1", sessionId);

        SubmitResponseBean submitResponseBean = submitForm("requests/submit/submit_request_case.json", sessionId);

        assertEquals("success", submitResponseBean.getStatus());

        // Test that we now have an additional case

        assertEquals(16, caseStorage.getNumRecords());

        // Try updating case

        NewFormResponse newSessionResponse1 = startNewForm("requests/new_form/new_form_4.json", "xforms/cases/update_case.xml");
        sessionId = newSessionResponse1.getSessionId();

        FormEntryResponseBean responseBean = answerQuestionGetResult("0", "Test Response", sessionId);
        QuestionBean firstResponseBean = responseBean.getTree()[0];
        assertEquals("Test Response", firstResponseBean.getAnswer());

        responseBean = answerQuestionGetResult("1", "1", sessionId);
        firstResponseBean = responseBean.getTree()[0];
        QuestionBean secondResponseBean = responseBean.getTree()[1];
        assertEquals(1, secondResponseBean.getAnswer());
        assertEquals("Test Response", firstResponseBean.getAnswer());

        answerQuestionGetResult("2", "[1, 2, 3]", sessionId);
        FormEntryResponseBean caseResult = answerQuestionGetResult("5", "2016-02-09", sessionId);
    }

    @Test
    public void testCaseClose() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_4.json", "xforms/cases/close_case.xml");

        UserSqlSandbox sandbox = getRestoreSandbox();
        SqlStorage<Case> caseStorage =  sandbox.getCaseStorage();
        assertEquals(15, caseStorage.getNumRecords());

        String sessionId = newSessionResponse.getSessionId();
        answerQuestionGetResult("0", "1", sessionId);

        SubmitResponseBean submitResponseBean = submitForm("requests/submit/submit_request_not_prevalidated.json", sessionId);
        assertEquals(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE, submitResponseBean.getStatus());

        submitResponseBean = submitForm("requests/submit/submit_request_bad.json", sessionId);
        assertEquals(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE, submitResponseBean.getStatus());
        assertEquals(1, submitResponseBean.getErrors().keySet().size());
        assertEquals("illegal-argument", submitResponseBean.getErrors().get("0").getType());

        submitResponseBean = submitForm("requests/submit/submit_request_close_case.json", sessionId);
        assertEquals(Constants.SUBMIT_RESPONSE_STATUS_POSITIVE, submitResponseBean.getStatus());

        // test that we have successfully closed this case (will still be in storage)
        caseStorage.removeAll(new CasePurgeFilter(caseStorage, null));
        Iterator caseIterator = caseStorage.iterator();
        int openCount = 0;
        while (caseIterator.hasNext()) {
            Case cCase = (Case)caseIterator.next();
            if (!cCase.isClosed()) {
                openCount ++;
            }
        }
        assertEquals(14, openCount);
    }

    @Test
    public void testEvaluateInstance() throws Exception {
        NewFormResponse newSessionResponse2 = startNewForm("requests/new_form/new_form_4.json", "xforms/cases/update_case.xml");

        // Aside: test EvaluateXPath with instance() and multiple matching nodes works
        EvaluateXPathResponseBean evaluateXPathResponseBean =
                evaluateXPath(newSessionResponse2.getSessionId(), "instance('casedb')/casedb/case/@case_id");

        assertEquals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE, evaluateXPathResponseBean.getStatus());
    }
}
