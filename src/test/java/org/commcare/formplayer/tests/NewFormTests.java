package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.QuestionBean;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Created by willpride on 1/14/16.
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class NewFormTests extends BaseTestClass {

    @Test
    public void testNewForm() throws Exception {
        // setup files
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form.json",
                "xforms/basic.xml");

        assert (newSessionResponse.getTitle().equals("Basic Form"));
        assert (newSessionResponse.getLangs().length == 2);

        // tree parsing
        QuestionBean[] tree = newSessionResponse.getTree();
        QuestionBean firstQuestion = tree[0];

        assert (firstQuestion.getCaption().equals("Enter a name:"));
        assert (tree.length == 1);
        assert (firstQuestion.getIx().contains("0"));
        assert (firstQuestion.getDatatype().equals("str"));
    }

    @Test
    public void testNewForm2() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form_2.json",
                "xforms/question_types.xml");

        assert (newSessionResponse.getTitle().equals("Question Types"));
        assert (newSessionResponse.getLangs().length == 2);

        // tree parsing
        QuestionBean[] tree = newSessionResponse.getTree();
        assert (tree.length == 24);
        for (int i = 0; i < tree.length; i++) {
            QuestionBean currentBean = tree[i];
            switch (i) {
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
