package tests;

import application.SessionController;
import auth.HqAuth;
import beans.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableSession;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import repo.SessionRepo;
import services.RestoreService;
import services.XFormService;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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

        when(xFormServiceMock.getFormXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "xforms/repeat.xml"));

        String requestPayload = FileUtils.getFile(this.getClass(), "requests/new_form/new_form.json");

        NewSessionRequestBean newSessionRequestBean = new ObjectMapper().readValue(requestPayload,
                NewSessionRequestBean.class);

        MvcResult result = this.mockMvc.perform(
                post("/new_session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newSessionRequestBean))).andReturn();

        String responseBody = result.getResponse().getContentAsString();

        JSONObject jsonResponse = new JSONObject(responseBody);

        String sessionId = jsonResponse.getString("session_id");

        String newRepeatRequestPayload = FileUtils.getFile(this.getClass(), "requests/new_repeat/new_repeat.json");
        String deleteRepeatRequestPayload = FileUtils.getFile(this.getClass(), "requests/delete_repeat/delete_repeat.json");


        ObjectMapper mapper = new ObjectMapper();
        RepeatRequestBean newRepeatRequestBean = mapper.readValue(newRepeatRequestPayload,
                RepeatRequestBean.class);
        newRepeatRequestBean.setSessionId(sessionId);

        RepeatRequestBean repeatRequestBean = mapper.readValue(deleteRepeatRequestPayload,
                RepeatRequestBean.class);
        repeatRequestBean.setSessionId(sessionId);

        String newRepeatRequestString = mapper.writeValueAsString(newRepeatRequestBean);
        String deleteRepeatRequestString = mapper.writeValueAsString(repeatRequestBean);

        ResultActions repeatResult = mockMvc.perform(get("/new_repeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(newRepeatRequestString));

        RepeatResponseBean newRepeatResponseBean= mapper.readValue(repeatResult.andReturn().getResponse().getContentAsString(),
                RepeatResponseBean.class);

        QuestionBean[] tree = newRepeatResponseBean.getTree();

        assert(tree.length == 2);
        QuestionBean questionBean = tree[1];
        assert(questionBean.getChildren() != null);
        QuestionBean[] children = questionBean.getChildren();
        assert(children.length == 1);
        QuestionBean child = children[0];
        assert(child.getIx().contains("1_0,"));
        children = child.getChildren();
        assert(children.length == 1);
        child = children[0];
        assert(child.getIx().contains("1_0, 0,"));

        repeatResult = mockMvc.perform(get("/new_repeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(newRepeatRequestString));

        newRepeatResponseBean= mapper.readValue(repeatResult.andReturn().getResponse().getContentAsString(),
                RepeatResponseBean.class);
        tree = newRepeatResponseBean.getTree();
        assert(tree.length == 2);
        questionBean = tree[1];
        assert(questionBean.getChildren() != null);
        children = questionBean.getChildren();
        assert(children.length == 2);

        child = children[0];
        assert(child.getIx().contains("1_0,"));
        QuestionBean[] children2 = child.getChildren();
        assert(children2.length == 1);
        child = children2[0];
        assert(child.getIx().contains("1_0, 0,"));

        child = children[1];
        children2 = child.getChildren();
        assert(children2.length == 1);
        child = children2[0];
        assert(child.getIx().contains("1_1, 0,"));


        repeatResult = mockMvc.perform(get("/delete_repeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(deleteRepeatRequestString));

        RepeatResponseBean deleteRepeatResponseBean = mapper.readValue(repeatResult.andReturn().getResponse().getContentAsString(),
                RepeatResponseBean.class);

        tree = deleteRepeatResponseBean.getTree();
        assert(tree.length == 2);
        questionBean = tree[1];
        assert(questionBean.getChildren() != null);
        children = questionBean.getChildren();
        assert(children.length == 1);
    }
}