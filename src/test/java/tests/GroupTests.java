package tests;

import beans.FormEntryResponseBean;
import beans.NewFormResponse;
import beans.QuestionBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

import static org.mockito.Matchers.any;

/**
 * Fills out the "Basic Tests > Groups" Form from the QA plan.
 * Provides coverage of fixtures, group expansion, selects from itemsets, conditional selects
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class GroupTests extends BaseTestClass{

    @Before
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("grouptestdomain", "grouptestuser");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/ccqa.xml";
    }

    @Test
    public void testConditionalItemsets() throws Exception {

        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_group.json", "xforms/groups.xml");

        String sessionId = newSessionResponse.getSessionId();

        // test that our county list expanded properly and the city list has not
        QuestionBean groupBean = newSessionResponse.getTree()[3];
        QuestionBean countyBean = groupBean.getChildren()[1];
        QuestionBean labelBean = groupBean.getChildren()[2];
        QuestionBean cityBean = groupBean.getChildren()[3];

        assert countyBean.getChoices().length == 3;
        assert cityBean.getChoices().length == 0;
        assert labelBean.getCaption().equals("Selected county was: ");

        FormEntryResponseBean mAnswerBean = answerQuestionGetResult(countyBean.getIx(), "1", sessionId);
        // test that after making a selection our city list populates
        groupBean = mAnswerBean.getTree()[3];
        cityBean = groupBean.getChildren()[3];
        labelBean = groupBean.getChildren()[2];

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

    public String toPrettyTree(QuestionBean[] questionBean) {
        try {
            return new ObjectMapper().writeValueAsString(questionBean);
        } catch(JsonProcessingException e){
            return "Error: " + e;
        }
    }

    @Test
    public void testMultiSelectGroups() throws Exception {

        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_group.json", "xforms/groups.xml");

        String sessionId = newSessionResponse.getSessionId();

        QuestionBean groupBean = newSessionResponse.getTree()[1];
        assert groupBean.getType().equals("sub-group");
        QuestionBean[] children = groupBean.getChildren();
        assert children.length == 1;

        FormEntryResponseBean mAnswerBean = answerQuestionGetResult(children[0].getIx(), "1", sessionId);
        groupBean = mAnswerBean.getTree()[1];
        children = groupBean.getChildren();

        assert children.length == 2;
        assert children[1].getBinding().equals("/data/onepagegroup/multiple_text");
        assert children[1].getIx().contains("1,4");

        mAnswerBean = answerQuestionGetResult(children[0].getIx(), "2", sessionId);
        groupBean = mAnswerBean.getTree()[1];
        children = groupBean.getChildren();
        assert children.length == 2;
        assert children[1].getBinding().equals("/data/onepagegroup/multiple_select");
        assert children[1].getIx().contains("1,2");

        mAnswerBean = answerQuestionGetResult(children[1].getIx(), "3", sessionId);
        groupBean = mAnswerBean.getTree()[1];
        children = groupBean.getChildren();
        assert children.length == 3;
        assert children[2].getBinding().equals("/data/onepagegroup/multiple_sel_other");
        assert children[2].getIx().contains("1,3");
    }

    @Test
    public void testInnerOuterGroups() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_group.json", "xforms/groups.xml");
    }
}