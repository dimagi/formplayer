package tests;

import application.FormController;
import auth.HqAuth;
import beans.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableFormSession;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import repo.SessionRepo;
import services.RestoreService;
import services.SubmitService;
import services.XFormService;
import util.Constants;
import utils.FileUtils;

import javax.servlet.http.Cookie;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Created by willpride on 2/3/16.
 */
public class BaseTestClass {

    private MockMvc mockMvc;

    @Autowired
    private SessionRepo sessionRepoMock;

    @Autowired
    private XFormService xFormServiceMock;

    @Autowired
    RestoreService restoreServiceMock;

    @Autowired
    SubmitService submitServiceMock;

    @InjectMocks
    protected FormController formController;

    private ObjectMapper mapper;

    final SerializableFormSession serializableFormSession = new SerializableFormSession();

    @Before
    public void setUp() throws IOException {
        Mockito.reset(sessionRepoMock);
        Mockito.reset(xFormServiceMock);
        Mockito.reset(restoreServiceMock);
        Mockito.reset(submitServiceMock);
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(formController).build();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore_3.xml"));
        when(submitServiceMock.submitForm(anyString(), anyString(), any(HqAuth.class)))
                .thenReturn(new ResponseEntity<String>(HttpStatus.OK));
        mapper = new ObjectMapper();
        setUpSessionRepoMock();
    }

    private void setUpSessionRepoMock() {

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return serializableFormSession;
            }
        }).when(sessionRepoMock).findOne(anyString());

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

    private String urlPrepend(String string) {
        return "/" + string;
    }


    AnswerQuestionResponseBean answerQuestionGetResult(String index, String answer, String sessionId) throws Exception {
        AnswerQuestionRequestBean answerQuestionBean = new AnswerQuestionRequestBean(index, answer, sessionId);
        String result = generateMockQuery(RequestType.POST, Constants.URL_ANSWER_QUESTION, answerQuestionBean);
        return mapper.readValue(result, AnswerQuestionResponseBean.class);
    }

    NewFormSessionResponse startNewSession(String requestPath, String formPath) throws Exception {
        when(xFormServiceMock.getFormXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), formPath));
        String requestPayload = FileUtils.getFile(this.getClass(), requestPath);
        String result = generateMockQuery(RequestType.POST, Constants.URL_NEW_SESSION, requestPayload);
        return mapper.readValue(result, NewFormSessionResponse.class);
    }

    CaseFilterResponseBean filterCases(String requestPath) throws Exception {
        String filterRequestPayload = FileUtils.getFile(this.getClass(), requestPath);
        String result = generateMockQuery(RequestType.GET, Constants.URL_FILTER_CASES, filterRequestPayload);
        return mapper.readValue(result, CaseFilterResponseBean.class);
    }

    CaseFilterFullResponseBean filterCasesFull() throws Exception {
        String filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases.json");
        String result = generateMockQuery(RequestType.GET, Constants.URL_FILTER_CASES_FULL, filterRequestPayload);
        return mapper.readValue(result, CaseFilterFullResponseBean.class);
    }

    SubmitResponseBean submitForm(String requestPath, String sessionId) throws Exception {
        SubmitRequestBean submitRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), SubmitRequestBean.class);
        submitRequestBean.setSessionId(sessionId);
        String result = generateMockQuery(RequestType.POST, Constants.URL_SUBMIT_FORM, submitRequestBean);
        return mapper.readValue(result, SubmitResponseBean.class);
    }

    SyncDbResponseBean syncDb() throws Exception {
        String syncDbRequestPayload = FileUtils.getFile(this.getClass(), "requests/sync_db/sync_db.json");
        SyncDbRequestBean syncDbRequestBean = mapper.readValue(syncDbRequestPayload,
                SyncDbRequestBean.class);
        String syncResult = generateMockQuery(RequestType.POST, Constants.URL_SYNC_DB, syncDbRequestBean);
        return mapper.readValue(syncResult, SyncDbResponseBean.class);
    }

    RepeatResponseBean newRepeatRequest(String sessionId) throws Exception {
        String newRepeatRequestPayload = FileUtils.getFile(this.getClass(), "requests/new_repeat/new_repeat.json");
        RepeatRequestBean newRepeatRequestBean = mapper.readValue(newRepeatRequestPayload,
                RepeatRequestBean.class);
        newRepeatRequestBean.setSessionId(sessionId);
        String repeatResult = generateMockQuery(RequestType.POST, Constants.URL_NEW_REPEAT, newRepeatRequestBean);
        return mapper.readValue(repeatResult, RepeatResponseBean.class);
    }

    RepeatResponseBean deleteRepeatRequest(String sessionId) throws Exception {
        String deleteRepeatRequestPayload = FileUtils.getFile(this.getClass(), "requests/delete_repeat/delete_repeat.json");
        RepeatRequestBean deleteRepeatRequestBean = mapper.readValue(deleteRepeatRequestPayload,
                RepeatRequestBean.class);
        deleteRepeatRequestBean.setSessionId(sessionId);
        String repeatResult = generateMockQuery(RequestType.POST, Constants.URL_DELETE_REPEAT, deleteRepeatRequestBean);
        return mapper.readValue(repeatResult, RepeatResponseBean.class);
    }

    CurrentResponseBean getCurrent(String sessionId) throws Exception {
        CurrentRequestBean currentRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/current/current_request.json"), CurrentRequestBean.class);
        currentRequestBean.setSessionId(sessionId);
        String currentResultString = generateMockQuery(RequestType.GET, Constants.URL_CURRENT, currentRequestBean);
        return mapper.readValue(currentResultString, CurrentResponseBean.class);
    }

    GetInstanceResponseBean getInstance(String sessionId) throws Exception {
        GetInstanceRequestBean getInstanceRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/current/current_request.json"), GetInstanceRequestBean.class);
        getInstanceRequestBean.setSessionId(sessionId);
        String getInstanceResultString = generateMockQuery(RequestType.POST, Constants.URL_GET_INSTANCE, getInstanceRequestBean);
        return mapper.readValue(getInstanceResultString, GetInstanceResponseBean.class);
    }

    EvaluateXPathResponseBean evaluateXPath(String sessionId, String xPath) throws Exception {
        EvaluateXPathRequestBean evaluateXPathRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/evaluate_xpath/evaluate_xpath.json"), EvaluateXPathRequestBean.class);
        evaluateXPathRequestBean.setSessionId(sessionId);
        evaluateXPathRequestBean.setXpath(xPath);
        String evaluateXpathResultString = generateMockQuery(RequestType.GET, Constants.URL_EVALUATE_XPATH, evaluateXPathRequestBean);
        return mapper.readValue(evaluateXpathResultString,
                EvaluateXPathResponseBean.class);
    }

    public enum RequestType {
        POST, GET
    }

    private String generateMockQuery(RequestType requestType, String urlPath, Object bean) throws Exception {
        ResultActions evaluateXpathResult = null;
        if (!(bean instanceof String)) {
            bean = mapper.writeValueAsString(bean);
        }
        switch (requestType) {
            case POST:
                evaluateXpathResult = mockMvc.perform(
                        post(urlPrepend(urlPath))
                                .contentType(MediaType.APPLICATION_JSON)
                                .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                                .content((String) bean));
                break;

            case GET:
                evaluateXpathResult = mockMvc.perform(
                        get(urlPrepend(urlPath))
                                .contentType(MediaType.APPLICATION_JSON)
                                .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                                .content((String) bean));
                break;
        }
        return evaluateXpathResult.andReturn().getResponse().getContentAsString();
    }
}
