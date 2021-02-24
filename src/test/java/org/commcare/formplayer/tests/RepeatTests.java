package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.FormEntryResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.QuestionBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.commcare.formplayer.utils.TestContext;

/**
 * Created by willpride on 1/14/16.
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class RepeatTests extends BaseTestClass{

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("test", "test");
    }

    @Test
    public void testRepeat() throws Exception {

        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form.json", "xforms/repeat.xml");

        String sessionId = newSessionResponse.getSessionId();

        FormEntryResponseBean newRepeatResponseBean = newRepeatRequest(sessionId);

        QuestionBean[] tree = newRepeatResponseBean.getTree();

        assert(tree.length == 2);
        QuestionBean questionBean = tree[1];
        assert(questionBean.getChildren() != null);
        QuestionBean[] children = questionBean.getChildren();
        assert(children.length == 1);
        QuestionBean child = children[0];
        assert(child.getIx().contains("1_0"));
        children = child.getChildren();
        assert(children.length == 1);
        child = children[0];
        assert(child.getIx().contains("1_0,0"));


        newRepeatResponseBean = newRepeatRequest(sessionId);

        tree = newRepeatResponseBean.getTree();
        assert(tree.length == 2);
        questionBean = tree[1];
        assert(questionBean.getChildren() != null);
        children = questionBean.getChildren();
        assert(children.length == 2);

        child = children[0];
        assert(child.getIx().contains("1_0"));
        QuestionBean[] children2 = child.getChildren();
        assert(children2.length == 1);
        child = children2[0];
        assert(child.getIx().contains("1_0,0"));

        child = children[1];
        children2 = child.getChildren();
        assert(children2.length == 1);
        child = children2[0];
        assert(child.getIx().contains("1_1,0"));

        FormEntryResponseBean deleteRepeatResponseBean = deleteRepeatRequest(sessionId);

        tree = deleteRepeatResponseBean.getTree();
        assert(tree.length == 2);
        questionBean = tree[1];
        assert(questionBean.getChildren() != null);
        children = questionBean.getChildren();
        assert(children.length == 1);
    }
}