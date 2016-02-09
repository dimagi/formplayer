package tests;

import beans.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.FileUtils;
import utils.TestContext;

import static org.mockito.Matchers.any;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class FormEntryTest extends BaseTestClass{

    //Integration test of form entry functions
    @Test
    public void testFormEntry() throws Exception {

        serializableFormSession.setRestoreXml(FileUtils.getFile(this.getClass(), "test_restore.xml"));

        NewSessionResponse newSessionResponse = startNewSession("requests/new_form/new_form_2.json", "xforms/question_types.xml");

        String sessionId = newSessionResponse.getSessionId();

        AnswerQuestionResponseBean response = answerQuestionGetResult("1","William Pride", sessionId);

        response = answerQuestionGetResult("2","345", sessionId);
        response = answerQuestionGetResult("3","2.54", sessionId);
        response = answerQuestionGetResult("4","1970-10-23", sessionId);
        response = answerQuestionGetResult("6", "12:30:30", sessionId);
        response = answerQuestionGetResult("7", "ben rudolph", sessionId);
        response = answerQuestionGetResult("8","123456789", sessionId);
        response = answerQuestionGetResult("10", "2",sessionId);
        response = answerQuestionGetResult("11", "1 2 3", sessionId);

        //Test Current Session
        CurrentResponseBean currentResponseBean = getCurrent(sessionId);

        //Test Get Instance
        GetInstanceResponseBean getInstanceResponseBean = getInstance(sessionId);

        //Test Evaluate XPath
        EvaluateXPathResponseBean evaluateXPathResponseBean = evaluateXPath(sessionId, "/data/q_text");
        assert evaluateXPathResponseBean.getStatus().equals("success");
        assert evaluateXPathResponseBean.getOutput().equals("William Pride");

        //Test Submission
        SubmitResponseBean submitResponseBean = submitForm("requests/submit/submit_request.json", sessionId);
    }
}