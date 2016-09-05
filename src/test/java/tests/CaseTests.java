package tests;

import application.SQLiteProperties;
import beans.*;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import util.Constants;
import utils.TestContext;

/**
 * Created by willpride on 1/14/16.
 *
 * This test tests that we can create and delete a case via the form API
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CaseTests extends BaseTestClass {

    @Test
    public void testCases() throws Exception {

        // Start new session and submit create case form

        NewFormSessionResponse newSessionResponse = startNewSession("requests/new_form/new_form_3.json",
                "xforms/cases/create_case.xml");

        CaseFilterResponseBean caseFilterResponseBean = filterCases("requests/filter/filter_cases_5.json");

        assert(caseFilterResponseBean.getCases().length == 15);

        String sessionId = newSessionResponse.getSessionId();

        answerQuestionGetResult("0", "Tom Brady", sessionId, 1);
        answerQuestionGetResult("1", "1", sessionId, 2);

        SubmitResponseBean submitResponseBean = submitForm("requests/submit/submit_request_case.json", sessionId);

        assert submitResponseBean.getStatus().equals("success");

        // Test that we now have an additional case

        caseFilterResponseBean = filterCases("requests/filter/filter_cases_5.json");

        assert(caseFilterResponseBean.getCases().length == 16);

        // Try updating case

        NewFormSessionResponse newSessionResponse1 = startNewSession("requests/new_form/new_form_4.json", "xforms/cases/update_case.xml");
        sessionId = newSessionResponse1.getSessionId();

        FormEntryResponseBean responseBean = answerQuestionGetResult("0", "Test Response", sessionId, 1);
        QuestionBean firstResponseBean = responseBean.getTree()[0];
        assert firstResponseBean.getAnswer().equals("Test Response");

        responseBean = answerQuestionGetResult("1", "1", sessionId, 2);
        firstResponseBean = responseBean.getTree()[0];
        QuestionBean secondResponseBean = responseBean.getTree()[1];
        assert secondResponseBean.getAnswer().equals(1);
        assert firstResponseBean.getAnswer().equals("Test Response");

        answerQuestionGetResult("2", "[1, 2, 3]", sessionId, 3);
        FormEntryResponseBean caseResult = answerQuestionGetResult("5", "2016-02-09", sessionId, 4);
        QuestionBean[] tree = caseResult.getTree();

        // close this case

        NewFormSessionResponse newSessionResponse2 = startNewSession("requests/new_form/new_form_4.json", "xforms/cases/close_case.xml");

        assert(filterCases("requests/filter/filter_cases_5.json").getCases().length == 16);

        sessionId = newSessionResponse2.getSessionId();
        answerQuestionGetResult("0", "1", sessionId, 1);

        submitResponseBean = submitForm("requests/submit/submit_request_not_prevalidated.json", sessionId);
        assert submitResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE);

        submitResponseBean = submitForm("requests/submit/submit_request_bad.json", sessionId);
        assert submitResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE);
        assert submitResponseBean.getErrors().keySet().size() == 1;
        assert submitResponseBean.getErrors().get("0").getType().equals("illegal-argument");

        submitResponseBean = submitForm("requests/submit/submit_request_close_case.json", sessionId);
        assert submitResponseBean.getStatus().equals(Constants.SYNC_RESPONSE_STATUS_POSITIVE);

        // test that we have successfully removed this case

        assert(filterCases("requests/filter/filter_cases_5.json").getCases().length == 15);
    }

    @Test
    public void testSqlEscape() throws Exception {
        CaseFilterResponseBean caseFilterResponseBean = filterCases("requests/filter/filter_cases_sql_escape.json");
        assert(caseFilterResponseBean.getCases().length == 15);
    }



    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder(SQLiteProperties.getDataDir());
    }

}