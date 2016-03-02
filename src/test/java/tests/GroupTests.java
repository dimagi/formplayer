package tests;

import auth.HqAuth;
import beans.AnswerQuestionResponseBean;
import beans.NewFormSessionResponse;
import beans.QuestionBean;
import beans.RepeatResponseBean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;
import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Fills out the "Basic Tests > Groups" Form from the QA plan.
 * Provides coverage of fixtures, group expansion, selects from itemsets, conditional selects
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class GroupTests extends BaseTestClass{

    @Before
    public void setUp() throws IOException {
        super.setUp();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "restores/ccqa.xml"));
    }

    @Test
    public void testGroups() throws Exception {

        NewFormSessionResponse newSessionResponse = startNewSession("requests/new_form/new_form.json", "xforms/groups.xml");

        String sessionId = newSessionResponse.getSessionId();

        // test that our county list expanded properly and the city list has not
        QuestionBean groupBean = newSessionResponse.getTree()[3];
        QuestionBean countyBean = groupBean.getChildren()[1];
        QuestionBean labelBean = groupBean.getChildren()[2];
        QuestionBean cityBean = groupBean.getChildren()[3];

        assert countyBean.getChoices().length == 3;
        assert cityBean.getChoices().length == 0;
        assert labelBean.getCaption().equals("Selected county was: ");

        AnswerQuestionResponseBean mAnswerBean = answerQuestionGetResult(countyBean.getIx(), "1", sessionId);
        // test that after making a selection our city list populates
        groupBean = mAnswerBean.getTree()[3];
        cityBean = groupBean.getChildren()[3];
        labelBean = groupBean.getChildren()[2];

        System.out.println("City Bean: " + cityBean);
        assert labelBean.getCaption().equals("Selected county was: ex");
        assert cityBean.getChoices().length == 2;

        // test that when we change the answer the possible choices update
        mAnswerBean = answerQuestionGetResult(countyBean.getIx(), "2", sessionId);
        groupBean = mAnswerBean.getTree()[3];
        cityBean = groupBean.getChildren()[3];
        labelBean = groupBean.getChildren()[2];
        assert cityBean.getChoices().length == 3;
        assert labelBean.getCaption().equals("Selected county was: mx");

    }
}