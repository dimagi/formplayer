package tests;

import application.SQLiteProperties;
import beans.*;
import sandbox.SqlSandboxUtils;
import sandbox.SqliteIndexedStorageUtility;
import sandbox.UserSqlSandbox;
import org.commcare.cases.model.Case;
import org.commcare.cases.util.CasePurgeFilter;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import tests.sandbox.TestConnectionHandler;
import util.Constants;
import utils.TestContext;

import java.util.Iterator;

/**
 * Created by willpride on 1/14/16.
 *
 * This test tests that we can create and delete a case via the form API
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CaseTests extends BaseTestClass {

    @Test
    public void testCaseCreate() throws Exception {
        // Start new session and submit create case form
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_3.json",
                "xforms/cases/create_case.xml");

        UserSqlSandbox sandbox = restoreFactoryMock.getSqlSandbox();

        SqliteIndexedStorageUtility<Case> caseStorage =  sandbox.getCaseStorage();

        assert(caseStorage.getNumRecords() == 15);
        sandbox.getConnection().close();

        String sessionId = newSessionResponse.getSessionId();

        answerQuestionGetResult("0", "Tom Brady", sessionId);
        answerQuestionGetResult("1", "1", sessionId);

        SubmitResponseBean submitResponseBean = submitForm("requests/submit/submit_request_case.json", sessionId);

        assert submitResponseBean.getStatus().equals("success");

        // Test that we now have an additional case

        assert(caseStorage.getNumRecords()== 16);

        // Try updating case

        NewFormResponse newSessionResponse1 = startNewForm("requests/new_form/new_form_4.json", "xforms/cases/update_case.xml");
        sessionId = newSessionResponse1.getSessionId();

        FormEntryResponseBean responseBean = answerQuestionGetResult("0", "Test Response", sessionId);
        QuestionBean firstResponseBean = responseBean.getTree()[0];
        assert firstResponseBean.getAnswer().equals("Test Response");

        responseBean = answerQuestionGetResult("1", "1", sessionId);
        firstResponseBean = responseBean.getTree()[0];
        QuestionBean secondResponseBean = responseBean.getTree()[1];
        assert secondResponseBean.getAnswer().equals(1);
        assert firstResponseBean.getAnswer().equals("Test Response");

        answerQuestionGetResult("2", "[1, 2, 3]", sessionId);
        FormEntryResponseBean caseResult = answerQuestionGetResult("5", "2016-02-09", sessionId);
    }

    @Test
    public void testCaseClose() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_4.json", "xforms/cases/close_case.xml");

        UserSqlSandbox sandbox = restoreFactoryMock.getSqlSandbox();
        SqliteIndexedStorageUtility<Case> caseStorage =  sandbox.getCaseStorage();
        assert(caseStorage.getNumRecords() == 15);

        String sessionId = newSessionResponse.getSessionId();
        answerQuestionGetResult("0", "1", sessionId);

        SubmitResponseBean submitResponseBean = submitForm("requests/submit/submit_request_not_prevalidated.json", sessionId);
        assert submitResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE);

        submitResponseBean = submitForm("requests/submit/submit_request_bad.json", sessionId);
        assert submitResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE);
        assert submitResponseBean.getErrors().keySet().size() == 1;
        assert submitResponseBean.getErrors().get("0").getType().equals("illegal-argument");

        submitResponseBean = submitForm("requests/submit/submit_request_close_case.json", sessionId);
        assert submitResponseBean.getStatus().equals(Constants.SYNC_RESPONSE_STATUS_POSITIVE);

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
        assert openCount == 14;
    }

    @Test
    public void testEvaluateInstance() throws Exception {
        NewFormResponse newSessionResponse2 = startNewForm("requests/new_form/new_form_4.json", "xforms/cases/update_case.xml");

        // Aside: test EvaluateXPath with instance() and multiple matching nodes works
        EvaluateXPathResponseBean evaluateXPathResponseBean =
                evaluateXPath(newSessionResponse2.getSessionId(), "instance('casedb')/casedb/case/@case_id");

        assert evaluateXPathResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE);
    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder(SQLiteProperties.getDataDir());
    }

}