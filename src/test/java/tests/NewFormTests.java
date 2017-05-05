package tests;

import beans.NewFormResponse;
import beans.QuestionBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

import static org.mockito.Matchers.any;

/**
 * Created by willpride on 1/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class NewFormTests extends BaseTestClass{

    @Test
    public void testNewForm() throws Exception {
        // setup files
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form.json", "xforms/basic.xml");

        assert(newSessionResponse.getTitle().equals("Basic Form"));
        assert(newSessionResponse.getLangs().length == 2);

        // tree parsing
        QuestionBean[] tree = newSessionResponse.getTree();
        QuestionBean firstQuestion = tree[0];

        assert(firstQuestion.getCaption().equals("Enter a name:"));
        assert(tree.length == 1);
        assert(firstQuestion.getIx().contains("0"));
        assert(firstQuestion.getDatatype().equals("str"));
    }

    @Test
    public void testNewForm2() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_2.json", "xforms/question_types.xml");

        assert(newSessionResponse.getTitle().equals("Question Types"));
        assert(newSessionResponse.getLangs().length == 2);

        // tree parsing
        QuestionBean[] tree = newSessionResponse.getTree();
        assert(tree.length == 24);
        for(int i=0; i<tree.length; i++){
            QuestionBean currentBean = tree[i];
            switch(i){
                case 3:
                    assert currentBean.getBinding().equals("/data/q_numeric");
            }
        }
    }

    // Open a form that requires custom user data
    @Override
    protected String getMockRestoreFileName() {
        return "restores/edit_user_data.xml";
    }

    @Test
    public void editFormWithUserData() throws Exception {
        startNewForm("requests/new_form/edit_user_data.json", "xforms/edit_user_data.xml");
    }
}