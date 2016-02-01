package tests;

import application.CaseController;
import application.SessionController;
import auth.HqAuth;
import beans.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableSession;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.cases.model.Case;
import org.commcare.core.sandbox.SandboxUtils;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathFuncExpr;
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
import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by willpride on 1/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CaseTests {

    private MockMvc mockMvc;

    @Autowired
    private SessionRepo sessionRepoMock;

    @Autowired
    private XFormService xFormServiceMock;

    @Autowired
    private RestoreService restoreServiceMock;

    @InjectMocks
    private SessionController sessionController;

    ObjectMapper mapper;

    @Before
    public void setUp() throws IOException {
        Mockito.reset(sessionRepoMock);
        Mockito.reset(xFormServiceMock);
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(sessionController).build();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore_3.xml"));
        mapper = new ObjectMapper();
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
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
    public void testCases() throws Exception {

        final SerializableSession serializableSession =  new SerializableSession();

        when(sessionRepoMock.find(anyString())).thenReturn(serializableSession);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                SerializableSession toBeSaved = (SerializableSession) args[0];
                serializableSession.setInstanceXml(toBeSaved.getInstanceXml());
                serializableSession.setFormXml(toBeSaved.getFormXml());
                serializableSession.setRestoreXml(toBeSaved.getRestoreXml());
                serializableSession.setUsername(toBeSaved.getUsername());
                serializableSession.setSessionData(toBeSaved.getSessionData());
                return null;
            }
        }).when(sessionRepoMock).save(Matchers.any(SerializableSession.class));

        when(xFormServiceMock.getFormXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "xforms/cases/create_case.xml"));

        String requestPayload = FileUtils.getFile(this.getClass(), "requests/new_form/new_form_3.json");

        NewSessionRequestBean newSessionRequestBean = new ObjectMapper().readValue(requestPayload,
                NewSessionRequestBean.class);

        MvcResult result = this.mockMvc.perform(
                post("/new_session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newSessionRequestBean))).andReturn();

        String responseBody = result.getResponse().getContentAsString();

        JSONObject jsonResponse = new JSONObject(responseBody);

        String filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases_5.json");
        result = this.mockMvc.perform(
                post("/filter_cases_session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

        CaseFilterResponseBean caseFilterResponseBean0 = mapper.readValue(result.getResponse().getContentAsString(),
                CaseFilterResponseBean.class);
        String[] caseArray0 = caseFilterResponseBean0.getCases();

        System.out.println("Cases: " + Arrays.toString(caseArray0));

        assert(caseArray0.length == 15);

        String sessionId = jsonResponse.getString("session_id");

        answerQuestionGetResult("0", "Tom Brady", sessionId);
        answerQuestionGetResult("1", "1", sessionId);

        SubmitRequestBean submitRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/submit/submit_request_case.json"), SubmitRequestBean.class);
        submitRequestBean.setSessionId(sessionId);

        result = this.mockMvc.perform(
                post("/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(submitRequestBean))).andReturn();

        filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases_5.json");
        result = this.mockMvc.perform(
                post("/filter_cases_session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

        caseFilterResponseBean0 = mapper.readValue(result.getResponse().getContentAsString(),
                CaseFilterResponseBean.class);
        caseArray0 = caseFilterResponseBean0.getCases();

        assert(caseArray0.length == 16);

        final SerializableSession serializableSession2 =  new SerializableSession();

        when(sessionRepoMock.find(anyString())).thenReturn(serializableSession2);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                SerializableSession toBeSaved = (SerializableSession) args[0];
                serializableSession2.setInstanceXml(toBeSaved.getInstanceXml());
                serializableSession2.setFormXml(toBeSaved.getFormXml());
                serializableSession2.setRestoreXml(toBeSaved.getRestoreXml());
                serializableSession2.setUsername(toBeSaved.getUsername());
                serializableSession2.setSessionData(toBeSaved.getSessionData());
                return null;
            }
        }).when(sessionRepoMock).save(Matchers.any(SerializableSession.class));

        when(xFormServiceMock.getFormXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "xforms/cases/close_case.xml"));

        requestPayload = FileUtils.getFile(this.getClass(), "requests/new_form/new_form_4.json");

        newSessionRequestBean = new ObjectMapper().readValue(requestPayload,
                NewSessionRequestBean.class);

        result = this.mockMvc.perform(
                post("/new_session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newSessionRequestBean))).andReturn();

        responseBody = result.getResponse().getContentAsString();

        jsonResponse = new JSONObject(responseBody);

        filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases_5.json");
        result = this.mockMvc.perform(
                post("/filter_cases_session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

        caseFilterResponseBean0 = mapper.readValue(result.getResponse().getContentAsString(),
                CaseFilterResponseBean.class);
        caseArray0 = caseFilterResponseBean0.getCases();

        assert(caseArray0.length == 16);

        sessionId = jsonResponse.getString("session_id");
        answerQuestionGetResult("0", "1", sessionId);

        submitRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/submit/submit_request_case.json"), SubmitRequestBean.class);
        submitRequestBean.setSessionId(sessionId);

        result = this.mockMvc.perform(
                post("/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(submitRequestBean))).andReturn();

        filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases_5.json");

        result = this.mockMvc.perform(
                post("/filter_cases_session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

        caseFilterResponseBean0 = mapper.readValue(result.getResponse().getContentAsString(),
                CaseFilterResponseBean.class);
        caseArray0 = caseFilterResponseBean0.getCases();

        assert(caseArray0.length == 15);


    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
    }

}