package tests;

import application.SessionController;
import beans.AnswerQuestionRequestBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableSession;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import repo.SessionRepo;
import services.XFormService;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class AnswerQuestionControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private SessionRepo sessionRepoMock;

    @Autowired
    private XFormService xFormServiceMock;

    @InjectMocks
    private SessionController sessionController;

    @Before
    public void setUp() throws IOException {
        Mockito.reset(sessionRepoMock);
        Mockito.reset(xFormServiceMock);
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(sessionController).build();
    }

    @Test
    public void basicForm() throws Exception {

        SerializableSession serializableSession = new SerializableSession();
        serializableSession.setFormXml(FileUtils.getFile(this.getClass(), "xforms/basic.xml"));
        serializableSession.setInstanceXml(FileUtils.getFile(this.getClass(), "instances/basic_0.xml"));
        serializableSession.setId("test_id");

        when(sessionRepoMock.find(anyString()))
                .thenReturn(serializableSession);

        AnswerQuestionRequestBean answerQuestionBean = new AnswerQuestionRequestBean("0", "123", "test_id");
        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(answerQuestionBean);

        MvcResult answerResult = this.mockMvc.perform(
                post("/answer_question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject object = new JSONObject(answerResult.getResponse().getContentAsString());
        assert(object.has("tree"));
        String treeString = object.getString("tree");
        assert (object.get("status").equals("success"));
        System.out.println("answer response: " + treeString);
        JSONArray tree = new JSONArray(treeString);
        assert(tree.length() == 1);
        String jsonString = tree.getString(0);
        JSONObject jsonObject = new JSONObject(jsonString);
        assert(jsonObject.get("answer").equals("123"));

    }
}
