package tests;

import beans.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import util.Constants;
import utils.FileUtils;
import utils.TestContext;

import java.util.ArrayList;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class FormEntryTest extends BaseTestClass{

    //Integration test of form entry functions
    @Test
    public void testFormEntry() throws Exception {

        configureRestoreFactory("test", "test");

        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_2.json", "xforms/question_types.xml");

        String sessionId = newSessionResponse.getSessionId();

        FormEntryResponseBean response = answerQuestionGetResult("1","William Pride", sessionId);
        response = answerQuestionGetResult("2","345", sessionId);
        response = answerQuestionGetResult("3","2.54", sessionId);
        response = answerQuestionGetResult("4","1970-10-23", sessionId);
        response = answerQuestionGetResult("6", "12:30:30", sessionId);
        response = answerQuestionGetResult("7", "ben rudolph", sessionId);
        response = answerQuestionGetResult("8","123456789", sessionId);
        response = answerQuestionGetResult("10", "2",sessionId);
        response = answerQuestionGetResult("11", "1 2 3", sessionId);

        QuestionBean[] tree = response.getTree();

        QuestionBean textBean = tree[1];
        assert textBean.getAnswer().equals("William Pride");

        QuestionBean intBean = tree[2];
        assert intBean.getAnswer().equals(345);

        QuestionBean decimalBean = tree[3];
        assert decimalBean.getAnswer().equals(2.54);

        QuestionBean dateBean = tree[4];
        assert dateBean.getAnswer().equals("1970-10-23");

        QuestionBean multiSelectQuestion = tree[11];
        assert(multiSelectQuestion.getAnswer() instanceof ArrayList);
        ArrayList<Integer> answer = (ArrayList<Integer>) multiSelectQuestion.getAnswer();
        assert(answer.size() == 3);
        assert answer.get(0).equals(1);

        response = answerQuestionGetResult("12", "1", sessionId);
        tree = response.getTree();
        multiSelectQuestion = tree[11];
        assert(multiSelectQuestion.getAnswer() instanceof ArrayList);
        answer = (ArrayList<Integer>) multiSelectQuestion.getAnswer();
        assert(answer.size() == 3);
        assert answer.get(0).equals(1);

        response = answerQuestionGetResult("17", "[13.803252972154226, 7.723388671875]", sessionId);
        QuestionBean geoBean = response.getTree()[17];
        assert geoBean.getAnswer() instanceof  ArrayList;
        ArrayList<Double> geoCoordinates = (ArrayList<Double>) geoBean.getAnswer();
        Double latitude = geoCoordinates.get(0);
        assert latitude.equals(13.803252972154226);
        Double longitude = geoCoordinates.get(1);
        assert longitude.equals(7.723388671875);

        //Test Evaluate XPath
        EvaluateXPathResponseBean evaluateXPathResponseBean = evaluateXPath(sessionId, "/data/q_text");
        assert evaluateXPathResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE);
        assert evaluateXPathResponseBean.getOutput().equals("William Pride");

        // We shouldn't error when a path doesn't exist
        evaluateXPathResponseBean = evaluateXPath(sessionId, "/data/not_broken");
        assert evaluateXPathResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE);

        // However, we should error when the path is invalid
        evaluateXPathResponseBean = evaluateXPath(sessionId, "!data/broken");
        assert evaluateXPathResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_NEGATIVE);

        //Test Submission
        SubmitResponseBean submitResponseBean = submitForm("requests/submit/submit_request.json", sessionId);
    }


    //Integration test of form entry functions
    @Test
    public void testFormEntry2() throws Exception {

        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_2.json", "xforms/question_types_2.xml");

        String sessionId = newSessionResponse.getSessionId();

        answerQuestionGetResult("1","William Pride", sessionId);

        answerQuestionGetResult("8,1","1", sessionId);
        FormEntryResponseBean response = answerQuestionGetResult("8,2","2", sessionId);

        QuestionBean questionBean = response.getTree()[8];
        QuestionBean[] children = questionBean.getChildren();

        assert children.length == 4;

        QuestionBean red = children[1];
        QuestionBean blue = children[2];

        assert red.getAnswer().equals(1);
        assert blue.getAnswer().equals(2);

        response = answerQuestionGetResult("8,3","2", sessionId);

        questionBean = response.getTree()[8];
        children = questionBean.getChildren();

        red = children[1];
        blue = children[2];
        QuestionBean green = children[3];

        assert red.getAnswer().equals(1);
        assert blue.getAnswer().equals(2);
        assert green.getAnswer().equals(2);

    }

    // Tests for OQPS mode
    @Test
    public void testOQPS() throws Exception {

        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_oqps.json", "xforms/oqps.xml");

        assert newSessionResponse.getTree().length == 1;
    }

    // Tests for OQPS next and previous methods
    @Test
    public void testOQPSPreviousNext() throws Exception {

        NewFormResponse newFormResponse = startNewForm("requests/new_form/new_form_oqps.json", "xforms/oqps.xml");

        String sessionId = newFormResponse.getSessionId();

        assert newFormResponse.getTree().length == 1;

        FormEntryNavigationResponseBean response1 = nextScreen(sessionId);
        assert response1.getTree().length == 1;

        FormEntryNavigationResponseBean response2 = nextScreen(sessionId);
        assert response2.getTree().length == 3;

        FormEntryNavigationResponseBean response3 = previousScreen(sessionId);
        assert response3.getTree().length == 1;

    }
}