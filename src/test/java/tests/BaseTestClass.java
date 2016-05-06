package tests;

import application.FormController;
import auth.HqAuth;
import beans.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableFormSession;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.junit.After;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import repo.SessionRepo;
import services.RestoreService;
import services.XFormService;
import util.Constants;
import utils.FileUtils;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by willpride on 2/3/16.
 */
public class BaseTestClass {

    protected MockMvc mockMvc;

    @Autowired
    protected SessionRepo sessionRepoMock;

    @Autowired
    protected XFormService xFormServiceMock;

    @Autowired
    protected RestoreService restoreServiceMock;

    @InjectMocks
    protected FormController formController;

    ObjectMapper mapper;

    final protected SerializableFormSession serializableFormSession = new SerializableFormSession();

    @Before
    public void setUp() throws IOException {
        Mockito.reset(sessionRepoMock);
        Mockito.reset(xFormServiceMock);
        Mockito.reset(restoreServiceMock);
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(formController).build();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore_3.xml"));
        mapper = new ObjectMapper();
        setUpSessionRepoMock();
    }

    public void setUpSessionRepoMock(){

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return serializableFormSession;
            }
        }).when(sessionRepoMock).find(anyString());

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                SerializableFormSession toBeSaved = (SerializableFormSession) args[0];
                serializableFormSession.setInstanceXml(toBeSaved.getInstanceXml());
                serializableFormSession.setFormXml(toBeSaved.getFormXml());
                serializableFormSession.setRestoreXml(toBeSaved.getRestoreXml());
                serializableFormSession.setUsername(toBeSaved.getUsername());
                serializableFormSession.setSessionData(toBeSaved.getSessionData());
                serializableFormSession.setDomain(toBeSaved.getDomain());
                return null;
            }
        }).when(sessionRepoMock).save(Matchers.any(SerializableFormSession.class));
    }

    private String urlPrepend(String string){
        return "/" + string;
    }


    public AnswerQuestionResponseBean answerQuestionGetResult(String index, String answer, String sessionId) throws Exception {
        AnswerQuestionRequestBean answerQuestionBean = new AnswerQuestionRequestBean(index, answer, sessionId);
        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(answerQuestionBean);
        MvcResult answerResult = this.mockMvc.perform(
                post(urlPrepend(Constants.URL_ANSWER_QUESTION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andReturn();

        AnswerQuestionResponseBean response = mapper.readValue(answerResult.getResponse().getContentAsString(),
                AnswerQuestionResponseBean.class);
        return response;
    }

    public NewFormSessionResponse startNewSession(String requestPath, String formPath) throws Exception {

        when(xFormServiceMock.getFormXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), formPath));
        String requestPayload = FileUtils.getFile(this.getClass(), requestPath);

        NewSessionRequestBean newSessionRequestBean = mapper.readValue(requestPayload,
                NewSessionRequestBean.class);
        MvcResult result = this.mockMvc.perform(
                post(urlPrepend(Constants.URL_NEW_SESSION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(newSessionRequestBean))).andReturn();
        String responseBody = result.getResponse().getContentAsString();
        NewFormSessionResponse newSessionResponse = mapper.readValue(responseBody, NewFormSessionResponse.class);
        return newSessionResponse;
    }

    public CaseFilterResponseBean filterCases(String requestPath) throws Exception {

        String filterRequestPayload = FileUtils.getFile(this.getClass(), requestPath);
        MvcResult result = this.mockMvc.perform(
                get(urlPrepend(Constants.URL_FILTER_CASES))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

         return mapper.readValue(result.getResponse().getContentAsString(),
                CaseFilterResponseBean.class);
    }

    public CaseFilterFullResponseBean filterCasesFull(String requestPath) throws Exception {

        String filterRequestPayload = FileUtils.getFile(this.getClass(), requestPath);
        MvcResult result = this.mockMvc.perform(
                get(urlPrepend(Constants.URL_FILTER_CASES_FULL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

        return mapper.readValue(result.getResponse().getContentAsString(),
                CaseFilterFullResponseBean.class);
    }

    public SubmitResponseBean submitForm(String requestPath, String sessionId) throws Exception {
        SubmitRequestBean submitRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), SubmitRequestBean.class);
        submitRequestBean.setSessionId(sessionId);

        String result = this.mockMvc.perform(
                post(urlPrepend(Constants.URL_SUBMIT_FORM))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(submitRequestBean)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return mapper.readValue(result, SubmitResponseBean.class);
    }

    public SyncDbResponseBean syncDb(String requestPath) throws Exception {
        String syncDbRequestPayload = FileUtils.getFile(this.getClass(), "requests/sync_db/sync_db.json");

        SyncDbRequestBean syncDbRequestBean = mapper.readValue(syncDbRequestPayload,
                SyncDbRequestBean.class);

        MvcResult result = this.mockMvc.perform(
                post(urlPrepend(Constants.URL_SYNC_DB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(syncDbRequestBean)))
                .andExpect(status().isOk())
                .andReturn();

        SyncDbResponseBean syncDbResponseBean = mapper.readValue(result.getResponse().getContentAsString(),
                SyncDbResponseBean.class);
        return syncDbResponseBean;
    }

    public RepeatResponseBean newRepeatRequest(String path, String sessionId) throws Exception {

        String newRepeatRequestPayload = FileUtils.getFile(this.getClass(), path);

        RepeatRequestBean newRepeatRequestBean = mapper.readValue(newRepeatRequestPayload,
                RepeatRequestBean.class);
        newRepeatRequestBean.setSessionId(sessionId);

        String newRepeatRequestString = mapper.writeValueAsString(newRepeatRequestBean);

        String repeatResult = mockMvc.perform(
                post(urlPrepend(Constants.URL_NEW_REPEAT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(newRepeatRequestString)).andReturn().getResponse().getContentAsString();
        return mapper.readValue(repeatResult, RepeatResponseBean.class);
    }

    public RepeatResponseBean deleteRepeatRequest(String path, String sessionId) throws Exception {

        String newRepeatRequestPayload = FileUtils.getFile(this.getClass(), path);

        RepeatRequestBean newRepeatRequestBean = mapper.readValue(newRepeatRequestPayload,
                RepeatRequestBean.class);
        newRepeatRequestBean.setSessionId(sessionId);

        String newRepeatRequestString = mapper.writeValueAsString(newRepeatRequestBean);

        String repeatResult = mockMvc.perform(
                post(urlPrepend(Constants.URL_DELETE_REPEAT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(newRepeatRequestString)).andReturn().getResponse().getContentAsString();
        return mapper.readValue(repeatResult, RepeatResponseBean.class);
    }

    public CurrentResponseBean getCurrent(String sessionId) throws Exception{
        CurrentRequestBean currentRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/current/current_request.json"), CurrentRequestBean.class);
        currentRequestBean.setSessionId(sessionId);

        ResultActions currentResult = mockMvc.perform(
                get(urlPrepend(Constants.URL_CURRENT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(currentRequestBean)));
        String currentResultString = currentResult.andReturn().getResponse().getContentAsString();
        return mapper.readValue(currentResultString, CurrentResponseBean.class);
    }

    public GetInstanceResponseBean getInstance(String sessionId) throws Exception{
        GetInstanceRequestBean getInstanceRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/current/current_request.json"), GetInstanceRequestBean.class);
        getInstanceRequestBean.setSessionId(sessionId);
        ResultActions getInstanceResult = mockMvc.perform(
                get(urlPrepend(Constants.URL_GET_INSTANCE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(getInstanceRequestBean)));
        String getInstanceResultString = getInstanceResult.andReturn().getResponse().getContentAsString();
        return mapper.readValue(getInstanceResultString, GetInstanceResponseBean.class);
    }

    public EvaluateXPathResponseBean evaluateXPath(String sessionId, String xPath) throws Exception{
        EvaluateXPathRequestBean evaluateXPathRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/evaluate_xpath/evaluate_xpath.json"), EvaluateXPathRequestBean.class);
        evaluateXPathRequestBean.setSessionId(sessionId);
        evaluateXPathRequestBean.setXpath(xPath);
        ResultActions evaluateXpathResult = mockMvc.perform(
                get(urlPrepend(Constants.URL_EVALUATE_XPATH))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(evaluateXPathRequestBean)));
        String evaluateXpathResultString = evaluateXpathResult.andReturn().getResponse().getContentAsString();
        EvaluateXPathResponseBean evaluateXPathResponseBean = mapper.readValue(evaluateXpathResultString,
                EvaluateXPathResponseBean.class);
        return evaluateXPathResponseBean;
    }
}
