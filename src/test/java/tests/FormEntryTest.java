package tests;

import application.Application;
import auth.HqAuth;
import beans.AnswerQuestionRequestBean;
import beans.AnswerQuestionResponseBean;
import beans.CurrentRequestBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.web.context.WebApplicationContext;
import repo.SessionRepo;
import services.XFormService;
import utils.FileUtils;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringApplicationConfiguration(classes = Application.class)   // 2
@WebAppConfiguration   // 3
@IntegrationTest("server.port:0")   // 4
@RunWith(MockitoJUnitRunner.class)
public class FormEntryTest {

    @Mock
    XFormService xFormServiceMock;

    private MockMvc mockMvc;
 
    @Value("${local.server.port}")   // 6
    int port;
 
    @Before
    public void setUp() {
        WebApplicationContext context = (WebApplicationContext) SpringApplication.run(Application.class);
        mockMvc = MockMvcBuilders.<StandaloneMockMvcBuilder>webAppContextSetup(context).build();
    }

    public AnswerQuestionResponseBean answerQuestionGetResult(String index, String answer, String sessionId) throws Exception {
        AnswerQuestionRequestBean answerQuestionBean = new AnswerQuestionRequestBean(index, answer, sessionId);
        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(answerQuestionBean);

        MvcResult answerResult = this.mockMvc.perform(
                post("/answer_question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andReturn();

        AnswerQuestionResponseBean response = mapper.readValue(answerResult.getResponse().getContentAsString(),
                AnswerQuestionResponseBean.class);
        return response;
    }

    //Integration test of form entry functions
    @Test
    public void testFormEntry() throws Exception {
        when(xFormServiceMock.getFormXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "xforms/question_types.xml"));
        String requestPayload = FileUtils.getFile(this.getClass(), "requests/new_form/new_form_2.json");

        JSONObject request = new JSONObject(requestPayload);

        ResultActions result = mockMvc.perform(post("/new_session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()));
        JSONObject resultJson = new JSONObject(result.andReturn().getResponse().getContentAsString());

        String sessionId = resultJson.getString("session_id");

        AnswerQuestionResponseBean response = answerQuestionGetResult("1","William Pride", sessionId);
        response = answerQuestionGetResult("2","345", sessionId);
        response = answerQuestionGetResult("3","2.54", sessionId);
        response = answerQuestionGetResult("4","1970-10-23", sessionId);
        response = answerQuestionGetResult("6", "12:30:30", sessionId);
        response = answerQuestionGetResult("7", "ben rudolph", sessionId);
        response = answerQuestionGetResult("8","123456789", sessionId);
        response = answerQuestionGetResult("10", "2",sessionId);
        response = answerQuestionGetResult("11", "1 2 3", sessionId);
        
        ObjectMapper mapper = new ObjectMapper();

        CurrentRequestBean currentRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/current/current_request.json"), CurrentRequestBean.class);
        currentRequestBean.setSessionId(sessionId);

        //Test Current Session
        ResultActions currentResult = mockMvc.perform(get("/current")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(currentRequestBean)));
        String currentResultString = currentResult.andReturn().getResponse().getContentAsString();
    }
}