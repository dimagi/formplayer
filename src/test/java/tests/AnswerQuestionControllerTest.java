package tests;

import application.SessionController;
import auth.HqAuth;
import beans.AnswerQuestionRequestBean;
import beans.AnswerQuestionResponseBean;
import beans.QuestionBean;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import repo.SessionRepo;
import services.RestoreService;
import services.XFormService;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

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

    @Autowired
    private RestoreService restoreServiceMock;

    @InjectMocks
    private SessionController sessionController;

    @Before
    public void setUp() throws IOException {
        Mockito.reset(sessionRepoMock);
        Mockito.reset(xFormServiceMock);
        Mockito.reset(restoreServiceMock);
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(sessionController).build();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"));

    }

    public AnswerQuestionResponseBean answerQuestionGetResult(String index, String answer, String sessionId) {
        try {
            AnswerQuestionRequestBean answerQuestionBean = new AnswerQuestionRequestBean(index, answer, sessionId);
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(answerQuestionBean);

            MvcResult answerResult = this.mockMvc.perform(
                    post("/answer_question")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody))
                    .andExpect(status().isOk())
                    .andReturn();
            System.out.println("Answer Response: " + answerResult.getResponse().getContentAsString());
            AnswerQuestionResponseBean object = mapper.readValue(answerResult.getResponse().getContentAsString(),
                    AnswerQuestionResponseBean.class);
            return object;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    public void basicForm() throws Exception {

        SerializableSession serializableSession = new SerializableSession();
        serializableSession.setFormXml(FileUtils.getFile(this.getClass(), "xforms/basic.xml"));
        serializableSession.setInstanceXml(FileUtils.getFile(this.getClass(), "instances/basic_0.xml"));
        serializableSession.setRestoreXml(FileUtils.getFile(this.getClass(), "test_restore.xml"));
        serializableSession.setId("test_id");

        when(sessionRepoMock.find(anyString()))
                .thenReturn(serializableSession);

        AnswerQuestionResponseBean object = answerQuestionGetResult("0","123","test_id");

        System.out.println("0: " + object.getTree()[0].toString());

        assert(object.getTree().length == 1);
        QuestionBean questionBean = object.getTree()[0];
        assert(questionBean.getAnswer().equals("123"));

    }

    @Test
    public void questionTypesForm() throws Exception {

        SerializableSession serializableSession = new SerializableSession();
        serializableSession.setFormXml(FileUtils.getFile(this.getClass(), "xforms/question_types.xml"));
        serializableSession.setInstanceXml(FileUtils.getFile(this.getClass(), "instances/question_types_0.xml"));
        serializableSession.setId("test_id");

        when(sessionRepoMock.find(anyString()))
                .thenReturn(serializableSession);

        AnswerQuestionResponseBean object = answerQuestionGetResult("1","William Pride","test_id");

        assert(object.getTree().length == 24);
        assert(object.getTree()[1].getAnswer().equals("William Pride"));

        object = answerQuestionGetResult("2","123","test_id");

        System.out.println("Tree: " + Arrays.toString(object.getTree()));

        assert(object.getTree()[1].getAnswer().equals("William Pride"));
        assert(String.valueOf(object.getTree()[2].getAnswer()).equals("123"));
        assert(object.getTree().length == 24);
    }
}
