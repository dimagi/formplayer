package org.commcare.formplayer.tests;

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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by willpride on 1/14/16.
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class RepeatTests extends BaseTestClass {

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

        assert (tree.length == 2);
        QuestionBean questionBean = tree[1];
        assert (questionBean.getChildren() != null);
        QuestionBean[] children = questionBean.getChildren();
        assert (children.length == 1);
        QuestionBean child = children[0];
        assert (child.getIx().contains("1_0"));
        children = child.getChildren();
        assert (children.length == 1);
        child = children[0];
        assert (child.getIx().contains("1_0,0"));


        newRepeatResponseBean = newRepeatRequest(sessionId);

        tree = newRepeatResponseBean.getTree();
        assert (tree.length == 2);
        questionBean = tree[1];
        assert (questionBean.getChildren() != null);
        children = questionBean.getChildren();
        assert (children.length == 2);

        child = children[0];
        assert (child.getIx().contains("1_0"));
        QuestionBean[] children2 = child.getChildren();
        assert (children2.length == 1);
        child = children2[0];
        assert (child.getIx().contains("1_0,0"));

        child = children[1];
        children2 = child.getChildren();
        assert (children2.length == 1);
        child = children2[0];
        assert (child.getIx().contains("1_1,0"));

        FormEntryResponseBean deleteRepeatResponseBean = deleteRepeatRequest(sessionId);

        tree = deleteRepeatResponseBean.getTree();
        assert (tree.length == 2);
        questionBean = tree[1];
        assert (questionBean.getChildren() != null);
        children = questionBean.getChildren();
        assert (children.length == 1);
    }

    @Test
    public void testRepeatModelIteration() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/new_form/new_form.json", "xforms/repeat_model_iteration.xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(newSessionResponse.getInstanceXml().getOutput().getBytes()));
        Node songRepeats = doc.getElementsByTagName("song_repeats").item(0);

        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList)xPath.compile("/data/song_repeats/item").evaluate(doc, XPathConstants.NODESET);
        assertEquals(2, nodeList.getLength());
        assertEquals("0", nodeList.item(0).getAttributes().getNamedItem("index").getNodeValue());
        assertEquals("1", nodeList.item(1).getAttributes().getNamedItem("index").getNodeValue());
    }
}
