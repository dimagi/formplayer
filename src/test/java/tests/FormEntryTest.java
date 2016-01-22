package tests;

import application.Application;
import auth.HqAuth;
import beans.*;
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
        System.out.println("session id: " + sessionId);

        AnswerQuestionResponseBean response = answerQuestionGetResult("1","William Pride", sessionId);

        System.out.println("result2: " + response);

        response = answerQuestionGetResult("2","345", sessionId);

        System.out.println("result3: " + response);

        response = answerQuestionGetResult("3","2.54", sessionId);

        System.out.println("result3: " + response);

        response = answerQuestionGetResult("4","1970-10-23", sessionId);

        System.out.println("result3: " + response);

        response = answerQuestionGetResult("6", "12:30:30", sessionId);

        System.out.println("result3: " + response);

        response = answerQuestionGetResult("7", "ben rudolph", sessionId);

        System.out.println("result3: " + response);

        response = answerQuestionGetResult("8","123456789", sessionId);

        System.out.println("result3: " + response);

        response = answerQuestionGetResult("10", "2",sessionId);

        System.out.println("result3: " + response);

        response = answerQuestionGetResult("11", "1 2 3", sessionId);

        System.out.println("result3: " + response);

        ObjectMapper mapper = new ObjectMapper();

        //Test Current Session
        CurrentRequestBean currentRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/current/current_request.json"), CurrentRequestBean.class);
        currentRequestBean.setSessionId(sessionId);

        ResultActions currentResult = mockMvc.perform(get("/current")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(currentRequestBean)));
        String currentResultString = currentResult.andReturn().getResponse().getContentAsString();
        System.out.println("Current Result: " + currentResultString);

        //Test Get Instance
        GetInstanceRequestBean getInstanceRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/current/current_request.json"), GetInstanceRequestBean.class);
        getInstanceRequestBean.setSessionId(sessionId);
        ResultActions getInstanceResult = mockMvc.perform(get("/get_instance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(getInstanceRequestBean)));
        String getInstanceResultString = getInstanceResult.andReturn().getResponse().getContentAsString();
        System.out.println("Get Instance Result: " + getInstanceResultString);

        //Test Get Instance
        EvaluateXPathRequestBean evaluateXPathRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/evaluate_xpath/evaluate_xpath.json"), EvaluateXPathRequestBean.class);
        evaluateXPathRequestBean.setSessionId(sessionId);
        ResultActions evaluateXpathResult = mockMvc.perform(get("/evaluate_xpath")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(evaluateXPathRequestBean)));
        String evaluateXpathResultString = evaluateXpathResult.andReturn().getResponse().getContentAsString();
        System.out.println("Evaluate Xpath Result: " + evaluateXpathResultString);

        //Test Submission
        SubmitRequestBean submitRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/submit/submit_request.json"), SubmitRequestBean.class);
        currentRequestBean.setSessionId(sessionId);

        ResultActions submitResult = mockMvc.perform(post("/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(currentRequestBean)));
        String submitResultString = submitResult.andReturn().getResponse().getContentAsString();
        System.out.println("Current Result: " + submitResultString);
    }
}