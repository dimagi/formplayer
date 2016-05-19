package tests;

import auth.HqAuth;
import beans.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by willpride on 1/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class RepeatTests extends BaseTestClass{

    @Before
    public void setUp() throws IOException {
        super.setUp();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"));
    }

    @Test
    public void testRepeat() throws Exception {

        NewFormSessionResponse newSessionResponse = startNewSession("requests/new_form/new_form.json", "xforms/repeat.xml");

        String sessionId = newSessionResponse.getSessionId();

        RepeatResponseBean newRepeatResponseBean = newRepeatRequest(sessionId);

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

        RepeatResponseBean deleteRepeatResponseBean = deleteRepeatRequest(sessionId);

        tree = deleteRepeatResponseBean.getTree();
        assert(tree.length == 2);
        questionBean = tree[1];
        assert(questionBean.getChildren() != null);
        children = questionBean.getChildren();
        assert(children.length == 1);
    }
}