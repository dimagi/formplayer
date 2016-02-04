package tests;

import auth.HqAuth;
import beans.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableSession;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MvcResult;
import utils.FileUtils;
import utils.TestContext;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by willpride on 1/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CaseTests extends BaseTestClass {

    @Test
    public void testCases() throws Exception {

        JSONObject newSessionResponse = startNewSession("requests/new_form/new_form_3.json",
                "xforms/cases/create_case.xml");

        CaseFilterResponseBean caseFilterResponseBean = filterCases("requests/filter/filter_cases_5.json");

        assert(caseFilterResponseBean.getCases().length == 15);

        String sessionId = newSessionResponse.getString("session_id");

        answerQuestionGetResult("0", "Tom Brady", sessionId);
        answerQuestionGetResult("1", "1", sessionId);

        SubmitResponseBean submitResponseBean = submitForm("requests/submit/submit_request_case.json", sessionId);

        //TODO test this

        caseFilterResponseBean = filterCases("requests/filter/filter_cases_5.json");

        assert(caseFilterResponseBean.getCases().length == 16);

        JSONObject jsonResponse = startNewSession("requests/new_form/new_form_4.json", "xforms/cases/close_case.xml");

        assert(filterCases("requests/filter/filter_cases_5.json").getCases().length == 16);

        sessionId = jsonResponse.getString("session_id");
        answerQuestionGetResult("0", "1", sessionId);

        submitResponseBean = submitForm("requests/submit/submit_request_case.json", sessionId);

        //TODO test this

        assert(filterCases("requests/filter/filter_cases_5.json").getCases().length == 15);


    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
    }

}