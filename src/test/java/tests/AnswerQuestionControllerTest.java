package tests;

import application.SessionController;
import auth.HqAuth;
import beans.AnswerQuestionRequestBean;
import beans.AnswerQuestionResponseBean;
import beans.QuestionBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableSession;
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
import services.RestoreService;
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

    public AnswerQuestionResponseBean answerQuestionGetResult(String index, String answer, String sessionId) throws Exception {
        AnswerQuestionRequestBean answerQuestionBean = new AnswerQuestionRequestBean(index, answer, sessionId);
        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(answerQuestionBean);

        System.out.println("JSON body: " + jsonBody);

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
        assert(String.valueOf(object.getTree()[2].getAnswer()).equals("123"));
        object = answerQuestionGetResult("3","1.2345","test_id");
        assert(String.valueOf(object.getTree()[3].getAnswer()).equals("1.2345"));
        object = answerQuestionGetResult("4","1970-10-23","test_id");
        assert(String.valueOf(object.getTree()[4].getAnswer()).equals("23/10/70"));
        object = answerQuestionGetResult("6", "12:30", "test_id");
        assert(String.valueOf(object.getTree()[6].getAnswer()).equals("12:30"));
        object = answerQuestionGetResult("7", "ben rudolph", "test_id");
        assert(String.valueOf(object.getTree()[7].getAnswer()).equals("ben rudolph"));
        object = answerQuestionGetResult("8","123456789", "test_id");
        assert(String.valueOf(object.getTree()[8].getAnswer()).equals("123456789"));
        object = answerQuestionGetResult("10", "2","test_id");
        assert(String.valueOf(object.getTree()[10].getAnswer()).equals("2"));
        object = answerQuestionGetResult("10", "1","test_id");
        assert(String.valueOf(object.getTree()[10].getAnswer()).equals("1"));
        object = answerQuestionGetResult("11", "1 2 3", "test_id");
        assert(String.valueOf(object.getTree()[11].getAnswer()).equals("[1, 2, 3]"));
    }

    @Test
    public void constraintsForm() throws Exception {

        SerializableSession serializableSession = new SerializableSession();
        serializableSession.setFormXml(FileUtils.getFile(this.getClass(), "xforms/constraints.xml"));
        serializableSession.setInstanceXml(FileUtils.getFile(this.getClass(), "instances/constraints.xml"));
        serializableSession.setId("test_id");

        when(sessionRepoMock.find(anyString()))
                .thenReturn(serializableSession);

        AnswerQuestionResponseBean object = answerQuestionGetResult("2","test","test_id");
        assert object.getTree() == null;
        assert object.getStatus().equals("error");
        assert object.getType().equals("restraint");
        assert object.getReason().contains("Please try something else and continue");

        object = answerQuestionGetResult("2","not test","test_id");
        assert object.getTree()[2].getAnswer().equals("not test");
        assert object.getStatus().equals("success");
        assert object.getType() == null;
        assert object.getReason() == null;


        object = answerQuestionGetResult("3","t","test_id");
        assert object.getTree() == null;
        assert object.getStatus().equals("error");
        assert object.getType().equals("restraint");
        assert object.getReason().contains("Please try your answer again");

        object = answerQuestionGetResult("3","1234567","test_id");
        assert object.getTree() == null;
        assert object.getStatus().equals("error");
        assert object.getType().equals("restraint");
        assert object.getReason().contains("Please try your answer again");

        object = answerQuestionGetResult("3","12345","test_id");
        assert object.getTree()[3].getAnswer().equals("12345");
        assert object.getStatus().equals("success");
        assert object.getType() == null;
        assert object.getReason() == null;


        object = answerQuestionGetResult("4","Will","test_id");
        assert object.getTree() == null;
        assert object.getStatus().equals("error");
        assert object.getType().equals("illegal-argument");
        assert object.getReason().contains("Invalid cast of data");

        object = answerQuestionGetResult("4","10","test_id");
        assert object.getTree() == null;
        assert object.getStatus().equals("error");
        assert object.getType().equals("restraint");
        assert object.getReason().contains("Your entry is invalid");

        object = answerQuestionGetResult("4","90000","test_id");
        assert object.getTree() == null;
        assert object.getStatus().equals("error");
        assert object.getType().equals("restraint");
        assert object.getReason().contains("Your entry is invalid");

        object = answerQuestionGetResult("4","100","test_id");
        assert String.valueOf(object.getTree()[4].getAnswer()).equals("100");
        assert object.getStatus().equals("success");
    }
}
