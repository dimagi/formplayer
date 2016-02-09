package tests;

import beans.CaseFilterResponseBean;
import beans.NewSessionResponse;
import beans.SubmitResponseBean;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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

        NewSessionResponse newSessionResponse = startNewSession("requests/new_form/new_form_3.json",
                "xforms/cases/create_case.xml");

        CaseFilterResponseBean caseFilterResponseBean = filterCases("requests/filter/filter_cases_5.json");

        assert(caseFilterResponseBean.getCases().length == 15);

        String sessionId = newSessionResponse.getSessionId();

        answerQuestionGetResult("0", "Tom Brady", sessionId);
        answerQuestionGetResult("1", "1", sessionId);

        SubmitResponseBean submitResponseBean = submitForm("requests/submit/submit_request_case.json", sessionId);

        assert submitResponseBean.getStatus().equals("success");

        // Test that we now have an additional case

        caseFilterResponseBean = filterCases("requests/filter/filter_cases_5.json");

        assert(caseFilterResponseBean.getCases().length == 16);

        // close this case

        NewSessionResponse newSessionResponse2 = startNewSession("requests/new_form/new_form_4.json", "xforms/cases/close_case.xml");

        assert(filterCases("requests/filter/filter_cases_5.json").getCases().length == 16);

        sessionId = newSessionResponse2.getSessionId();
        answerQuestionGetResult("0", "1", sessionId);

        submitResponseBean = submitForm("requests/submit/submit_request_case.json", sessionId);

        assert submitResponseBean.getStatus().equals("success");

        // test that we have successfully removed this case

        assert(filterCases("requests/filter/filter_cases_5.json").getCases().length == 15);


    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
    }

}