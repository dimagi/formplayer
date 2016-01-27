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
public class RepeatTests {

    final String NEW_FORM_URL = "/new_session";

    RestTemplate template = new TestRestTemplate();

    private MockMvc mockMvc;

    @Autowired
    private SessionRepo sessionRepoMock;

    @Autowired
    private XFormService xFormServiceMock;

    @Autowired
    private RestoreService restoreServiceMock;

    @InjectMocks
    private SessionController sessionController;

    @Before
    public void setUp() throws IOException {
        Mockito.reset(sessionRepoMock);
        Mockito.reset(xFormServiceMock);
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(sessionController).build();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"));
    }

    @Test
    public void testRepeat() throws Exception {

        final SerializableSession serializableSession =  new SerializableSession();

        when(sessionRepoMock.find(anyString())).thenReturn(serializableSession);

        ArgumentCaptor<SerializableSession> argumentCaptor = ArgumentCaptor.forClass(SerializableSession.class);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                SerializableSession toBeSaved = (SerializableSession) args[0];
                serializableSession.setInstanceXml(toBeSaved.getInstanceXml());
                serializableSession.setFormXml(toBeSaved.getFormXml());
                serializableSession.setRestoreXml(toBeSaved.getRestoreXml());
                return null;
            }
        }).when(sessionRepoMock).save(Matchers.any(SerializableSession.class));

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
        NewRepeatRequestBean newRepeatRequestBean = mapper.readValue(newRepeatRequestPayload,
                NewRepeatRequestBean.class);
        newRepeatRequestBean.setSessionId(sessionId);

        DeleteRepeatRequestBean deleteRepeatRequestBean = mapper.readValue(deleteRepeatRequestPayload,
                DeleteRepeatRequestBean.class);
        deleteRepeatRequestBean.setSessionId(sessionId);

        String newRepeatRequestString = mapper.writeValueAsString(newRepeatRequestBean);
        String deleteRepeatRequestString = mapper.writeValueAsString(deleteRepeatRequestBean);

        ResultActions repeatResult = mockMvc.perform(get("/new_repeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(newRepeatRequestString));

        NewRepeatResponseBean newRepeatResponseBean= mapper.readValue(repeatResult.andReturn().getResponse().getContentAsString(),
                NewRepeatResponseBean.class);

        System.out.println("First Response: " + repeatResult.andReturn().getResponse().getContentAsString());

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
        System.out.println("Child: " + child);
        assert(child.getIx().contains("1_0, 0,"));

        repeatResult = mockMvc.perform(get("/new_repeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(newRepeatRequestString));

        newRepeatResponseBean= mapper.readValue(repeatResult.andReturn().getResponse().getContentAsString(),
                NewRepeatResponseBean.class);
        tree = newRepeatResponseBean.getTree();
        assert(tree.length == 2);
        questionBean = tree[1];
        assert(questionBean.getChildren() != null);
        children = questionBean.getChildren();
        assert(children.length == 2);

        child = children[0];
        System.out.println("Child: " + child);
        assert(child.getIx().contains("1_0,"));
        QuestionBean[] children2 = child.getChildren();
        assert(children2.length == 1);
        child = children2[0];
        System.out.println("Child: " + child);
        assert(child.getIx().contains("1_0, 0,"));

        child = children[1];
        System.out.println("Child: " + child);
        children2 = child.getChildren();
        assert(children2.length == 1);
        child = children2[0];
        System.out.println("Child: " + child);
        assert(child.getIx().contains("1_1, 0,"));


        repeatResult = mockMvc.perform(get("/delete_repeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(deleteRepeatRequestString));

        DeleteRepeatResponseBean deleteRepeatResponseBean = mapper.readValue(repeatResult.andReturn().getResponse().getContentAsString(),
                DeleteRepeatResponseBean.class);

        tree = deleteRepeatResponseBean.getTree();
        assert(tree.length == 2);
        questionBean = tree[1];
        assert(questionBean.getChildren() != null);
        children = questionBean.getChildren();
        assert(children.length == 1);
    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
    }

}