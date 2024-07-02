package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.commcare.formplayer.beans.FormEntryResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.QuestionBean;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Created by willpride on 1/14/16.
 */
@WebMvcTest
public class RepeatTests extends BaseTestClass {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("test", "test");
    }

    @Test
    public void testRepeatNonCountedSimple() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form.json",
                "xforms/repeat.xml");
        QuestionBean[] tree = newSessionResponse.getTree();
        assert (tree.length == 2);
        QuestionBean dummyNode = tree[1];
        assertEquals("false", dummyNode.getExists());
        assertEquals("Add a new question3", dummyNode.getAddChoice());
        assertEquals("false", dummyNode.getExists());
        assertEquals(false, dummyNode.isDelete());

        String sessionId = newSessionResponse.getSessionId();
        FormEntryResponseBean newRepeatResponseBean = newRepeatRequest(sessionId, "1_0");

        // Verify the repeat has been added to form tree correctly
        tree = newRepeatResponseBean.getTree();
        assert (tree.length == 3);
        QuestionBean firstRepeat = tree[1];
        assertEquals("true", firstRepeat.getExists());
        assertEquals(true, firstRepeat.isDelete());
        assert (firstRepeat.getChildren().length == 1);
        QuestionBean[] children = firstRepeat.getChildren();
        assert (children.length == 1);
        QuestionBean child = children[0];
        assert (child.getIx().contains("1_0,0"));

        // verify that a second dummy repeat node is added with "exists=false"
        QuestionBean secondRepeat = tree[2];
        assertEquals("false", secondRepeat.getExists());
        assertEquals(false, secondRepeat.isDelete());
        assert (secondRepeat.getChildren().length == 0);
        assertEquals("Add another question3", secondRepeat.getAddChoice());

        // Add another repeat and verify the form tree accordingly
        newRepeatResponseBean = newRepeatRequest(sessionId, "1_1");
        tree = newRepeatResponseBean.getTree();
        assert (tree.length == 4);
        secondRepeat = tree[2];
        assertEquals("true", secondRepeat.getExists());
        assert (secondRepeat.getChildren().length == 1);
        children = secondRepeat.getChildren();
        assert (children[0].getIx().contains("1_1,0"));

        QuestionBean thirdRepeat = tree[3];
        assertEquals("false", thirdRepeat.getExists());
        assert (thirdRepeat.getChildren().length == 0);

        newRepeatRequest(sessionId, "1_2");
        answerQuestionGetResult("1_0,0", "repeat 1", sessionId);
        answerQuestionGetResult("1_1,0", "repeat 2", sessionId);
        answerQuestionGetResult("1_2,0", "repeat 3", sessionId);

        // delete second repeat
        FormEntryResponseBean deleteRepeatResponseBean = deleteRepeatRequest(sessionId,"1_1,0");

        // Verify that we deleted the repeat at right index
        tree = deleteRepeatResponseBean.getTree();
        assert (tree.length == 4);
        firstRepeat = tree[1];
        secondRepeat = tree[2];
        thirdRepeat = tree[3];
        assertEquals(firstRepeat.getChildren()[0].getAnswer(),"repeat 1");
        assertEquals(secondRepeat.getChildren()[0].getAnswer(),"repeat 3");
        assertEquals("false", thirdRepeat.getExists());


        // delete second repeat again from the new tree
        deleteRepeatResponseBean = deleteRepeatRequest(sessionId, "1_1,0");
        tree = deleteRepeatResponseBean.getTree();
        assert (tree.length == 3);
        firstRepeat = tree[1];
        secondRepeat = tree[2];
        assertEquals(firstRepeat.getChildren()[0].getAnswer(),"repeat 1");
        assertEquals("false", secondRepeat.getExists());
    }

    @Test
    public void testRepeatNonCountedNested() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form.json",
                "xforms/nested_repeat.xml");
        QuestionBean[] tree = newSessionResponse.getTree();
        assert (tree.length == 1);
        QuestionBean dummyNode = tree[0];
        assertEquals("false", dummyNode.getExists());

        String sessionId = newSessionResponse.getSessionId();
        FormEntryResponseBean newRepeatResponseBean = newRepeatRequest(sessionId, "0_0");
        tree = newRepeatResponseBean.getTree();

        // Verify the repeat has been added to form tree correctly
        assert (tree.length == 2);
        QuestionBean firstRepeat = tree[0];
        assertEquals("true", firstRepeat.getExists());
        assert (firstRepeat.getChildren().length == 1);
        QuestionBean[] children = firstRepeat.getChildren();
        assert (children.length == 1);
        QuestionBean child = children[0];
        assertEquals(child.getIx(), "0_0,0_0");
        assertEquals("false", child.getExists());

        // Child repeat request
        newRepeatResponseBean = newRepeatRequest(sessionId, "0_0, 0_0");
        tree = newRepeatResponseBean.getTree();
        assert (tree.length == 2);
        children = tree[0].getChildren();
        assert (children.length == 2);
        QuestionBean firstChild = children[0];
        assertEquals(firstChild.getIx(), "0_0,0_0");
        assertEquals("true", firstChild.getExists());
        QuestionBean secondChild = children[1];
        assertEquals(secondChild.getIx(), "0_0,0_1");
        assertEquals("false", secondChild.getExists());

        newRepeatRequest(sessionId, "0_0, 0_1");

        // create another parent repeat with three children
        newRepeatRequest(sessionId, "0_1");
        newRepeatRequest(sessionId, "0_1, 0_0");
        newRepeatRequest(sessionId, "0_1, 0_1");
        tree = newRepeatRequest(sessionId, "0_1, 0_2").getTree();

        // verify the form tree has 2 parent node with 2 and 3 children respectively
        // count below is count + 1 to account for dummy node
        assertEquals(3, tree.length);
        assertEquals (3, tree[0].getChildren().length);
        assertEquals (4, tree[1].getChildren().length);

        // Answer repeats
        answerQuestionGetResult("0_0,0_0,0", "repeat 1_1", sessionId);
        answerQuestionGetResult("0_0,0_1,0", "repeat 1_2", sessionId);
        answerQuestionGetResult("0_1,0_0,0", "repeat 2_1", sessionId);
        answerQuestionGetResult("0_1,0_1,0", "repeat 2_2", sessionId);
        answerQuestionGetResult("0_1,0_2,0", "repeat 2_3", sessionId);

        // delete 1_1 and 2_2 and verify the state
        deleteRepeatRequest(sessionId, "0_0,0_0");
        tree = deleteRepeatRequest(sessionId, "0_1,0_1").getTree();
        assertEquals(3, tree.length);
        assertEquals (2, tree[0].getChildren().length);
        assertEquals (3, tree[1].getChildren().length);
        assertEquals(tree[0].getChildren()[0].getChildren()[0].getAnswer(),"repeat 1_2");
        assertEquals(tree[0].getChildren()[1].getExists(),"false");
        assertEquals(tree[1].getChildren()[0].getChildren()[0].getAnswer(),"repeat 2_1");
        assertEquals(tree[1].getChildren()[1].getChildren()[0].getAnswer(),"repeat 2_3");
        assertEquals(tree[1].getChildren()[2].getExists(),"false");
    }

    @Test
    public void testRepeatModelIteration() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form.json",
                "xforms/repeat_model_iteration.xml");

        // counted repeat group nodes can't be deleted
        assertFalse(newSessionResponse.getTree()[1].isDelete());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(
                newSessionResponse.getInstanceXml().getOutput().getBytes()));
        Node songRepeats = doc.getElementsByTagName("song_repeats").item(0);

        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList)xPath.compile("/data/song_repeats/item").evaluate(doc,
                XPathConstants.NODESET);
        assertEquals(2, nodeList.getLength());
        assertEquals("0", nodeList.item(0).getAttributes().getNamedItem("index").getNodeValue());
        assertEquals("1", nodeList.item(1).getAttributes().getNamedItem("index").getNodeValue());
    }

    // tests a specific regression casued by stale repeat referenees after a delete repeat action
    @Test
    public void testNestedRepeatDeletionRegression() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form.json",
                "xforms/nested_repeat_deletion_regression.xml");
        String sessionId = newSessionResponse.getSessionId();

        // Add unit 1
        newRepeatRequest(sessionId, "0_0");
        answerQuestionGetResult("0_0,0,0", "unit 1", sessionId);

        // Add 2 beds bed 1 and bed 2
        newRepeatRequest(sessionId, "0_0,0,1,0_0");
        answerQuestionGetResult("0_0,0,1,0_0,0,0", "bed 1", sessionId);
        newRepeatRequest(sessionId, "0_0,0,1,0_1");
        QuestionBean[] tree = answerQuestionGetResult("0_0,0,1,0_1,0,0", "bed 2", sessionId).getTree();

        // verify state after 2 beds addition
        QuestionBean[] beds = tree[0].getChildren()[0].getChildren()[1].getChildren();
        assertEquals(3, beds.length);
        assertEquals("bed 1", beds[0].getChildren()[0].getChildren()[0].getAnswer());
        assertEquals("bed 2", beds[1].getChildren()[0].getChildren()[0].getAnswer());
        assertEquals("false", beds[2].getExists());

        // delete the bed 1
        tree = deleteRepeatRequest(sessionId, "0_0,0,1,0_0").getTree();

        // verify state after deletion
        beds = tree[0].getChildren()[0].getChildren()[1].getChildren();
        assertEquals(2, beds.length);
        assertEquals("bed 2", beds[0].getChildren()[0].getChildren()[0].getAnswer());
        assertEquals("false", beds[1].getExists());
    }
}
