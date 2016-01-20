package tests;

import application.SessionController;
import auth.HqAuth;
import beans.AnswerQuestionBean;
import beans.NewSessionBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableSession;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import repo.SessionRepo;
import services.XFormService;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    public void newSession() throws Exception {

        SerializableSession serializableSession = new SerializableSession();
        serializableSession.setInstanceXml(FileUtils.getFile(this.getClass(), "instances/basic_0.xml"));
        serializableSession.setId("test_id");


        when(sessionRepoMock.find(anyString()))
                .thenReturn(serializableSession);

        AnswerQuestionBean answerQuestionBean = new AnswerQuestionBean("0", "123", "test_id");
        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(answerQuestionBean);

        MvcResult answerResult = this.mockMvc.perform(
                post("/answer_question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject object = new JSONObject(answerResult.getResponse().getContentAsString());
        System.out.println("answer response: " + object);

    }
}
