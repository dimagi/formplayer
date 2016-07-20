package tests;

import application.FormController;
import application.MenuController;
import application.UtilController;
import auth.HqAuth;
import beans.*;
import beans.menus.CommandListResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import install.FormplayerConfigEngine;
import objects.SerializableFormSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import repo.SessionRepo;
import services.InstallService;
import services.RestoreService;
import services.SubmitService;
import services.XFormService;
import util.Constants;
import utils.FileUtils;

import javax.servlet.http.Cookie;
import java.io.File;
import java.io.IOException;
import java.net.URL;

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

    private MockMvc mockFormController;

    private MockMvc mockUtilController;

    private MockMvc mockMenuController;

    @Autowired
    private SessionRepo sessionRepoMock;

    @Autowired
    private XFormService xFormServiceMock;

    @Autowired
    RestoreService restoreServiceMock;

    @Autowired
    SubmitService submitServiceMock;

    @Autowired
    private InstallService installService;

    @InjectMocks
    protected FormController formController;

    @InjectMocks
    protected UtilController utilController;

    @InjectMocks
    protected MenuController menuController;

    protected ObjectMapper mapper;

    final SerializableFormSession serializableFormSession = new SerializableFormSession();

    @Before
    public void setUp() throws IOException {
        Mockito.reset(sessionRepoMock);
        Mockito.reset(xFormServiceMock);
        Mockito.reset(restoreServiceMock);
        Mockito.reset(submitServiceMock);
        Mockito.reset(installService);
        MockitoAnnotations.initMocks(this);
        mockFormController = MockMvcBuilders.standaloneSetup(formController).build();
        mockUtilController = MockMvcBuilders.standaloneSetup(utilController).build();
        mockMenuController = MockMvcBuilders.standaloneSetup(menuController).build();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"));
        when(submitServiceMock.submitForm(anyString(), anyString(), any(HqAuth.class)))
                .thenReturn(new ResponseEntity<String>(HttpStatus.OK));
        mapper = new ObjectMapper();
        setUpSessionRepoMock();
        setupInstallServiceMock();
    }

    private String resolveAppId(String ref){
        String appId = ref.substring(ref.indexOf("app_id=") + "app_id=".length(),
                ref.indexOf("#hack"));
        switch (appId) {
            case "doublemgmtappid":
                ref = "archives/parent_child.ccz";
                break;
            case "navigatorappid":
                ref = "archives/parent_child.ccz";
                break;
            case "caseappid":
                ref = "apps/basic2/profile.ccpr";
                break;
            case "createappid":
                ref = "archives/basic.ccz";
                break;
            case "casemediaappid":
                ref = "archives/casemedia.ccz";
                break;
            case "endformappid":
                ref = "archives/formnav.ccz";
                break;
            case "langsappid":
                ref = "archives/langs.ccz";
                break;
            default:
                throw new RuntimeException("Couldn't resolve appId for ref: " + ref);
        }
        return ref;
    }

    private void setupInstallServiceMock() {
        try {
            doAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    try {
                        Object[] args = invocationOnMock.getArguments();
                        String ref = (String) args[0];
                        if(ref.contains("#hack=commcare.ccz")){
                            ref = resolveAppId(ref);
                        }
                        String username = (String) args[1];
                        String path = (String) args[2];
                        FormplayerConfigEngine engine = new FormplayerConfigEngine(username, path);
                        String absolutePath = getTestResourcePath(ref);
                        System.out.println("Init with path: " + absolutePath);
                        if (absolutePath.endsWith(".ccpr")) {
                            engine.initFromLocalFileResource(absolutePath);
                        } else if (absolutePath.endsWith(".ccz")) {
                            engine.initFromArchive(absolutePath);
                        } else {
                            throw new RuntimeException("Can't install with reference: " + absolutePath);
                        }
                        engine.initEnvironment();
                        return engine;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
            }).when(installService).configureApplication(anyString(), anyString(), anyString());
        } catch(Exception e){
            // don't think we need error handling for mocking
            e.printStackTrace();
        }
    }

    private String getTestResourcePath(String resourcePath){
        try {
            URL url = this.getClass().getClassLoader().getResource(resourcePath);
            File file = new File(url.getPath());
            return file.getAbsolutePath();
        } catch(NullPointerException npe){
            npe.printStackTrace();
            throw npe;
        }
    }

    private void setUpSessionRepoMock(){

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

    private String urlPrepend(String string){
        return "/" + string;
    }


    AnswerQuestionResponseBean answerQuestionGetResult(String index, String answer, String sessionId) throws Exception {
        AnswerQuestionRequestBean answerQuestionBean = new AnswerQuestionRequestBean(index, answer, sessionId);
        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(answerQuestionBean);
        MvcResult answerResult = this.mockFormController.perform(
                post(urlPrepend(Constants.URL_ANSWER_QUESTION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andReturn();

        return mapper.readValue(answerResult.getResponse().getContentAsString(),
                AnswerQuestionResponseBean.class);
    }

    NewFormSessionResponse startNewSession(String requestPath, String formPath) throws Exception {

        when(xFormServiceMock.getFormXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), formPath));
        String requestPayload = FileUtils.getFile(this.getClass(), requestPath);

        NewSessionRequestBean newSessionRequestBean = mapper.readValue(requestPayload,
                NewSessionRequestBean.class);
        MvcResult result = this.mockFormController.perform(
                post(urlPrepend(Constants.URL_NEW_SESSION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                        .content(new ObjectMapper().writeValueAsString(newSessionRequestBean))).andReturn();
        String responseBody = result.getResponse().getContentAsString();
        return mapper.readValue(responseBody, NewFormSessionResponse.class);
    }

    CaseFilterResponseBean filterCases(String requestPath) throws Exception {

        String filterRequestPayload = FileUtils.getFile(this.getClass(), requestPath);
        MvcResult result = this.mockUtilController.perform(
                get(urlPrepend(Constants.URL_FILTER_CASES))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

         return mapper.readValue(result.getResponse().getContentAsString(),
                CaseFilterResponseBean.class);
    }

    CaseFilterFullResponseBean filterCasesFull() throws Exception {

        String filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases.json");
        MvcResult result = this.mockUtilController.perform(
                get(urlPrepend(Constants.URL_FILTER_CASES_FULL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filterRequestPayload))
                .andExpect(status().isOk())
                .andReturn();

        return mapper.readValue(result.getResponse().getContentAsString(),
                CaseFilterFullResponseBean.class);
    }

    SubmitResponseBean submitForm(String requestPath, String sessionId) throws Exception {
        SubmitRequestBean submitRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), SubmitRequestBean.class);
        submitRequestBean.setSessionId(sessionId);

        String result = this.mockFormController.perform(
                post(urlPrepend(Constants.URL_SUBMIT_FORM))
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                        .content(new ObjectMapper().writeValueAsString(submitRequestBean)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return mapper.readValue(result, SubmitResponseBean.class);
    }

    SyncDbResponseBean syncDb() throws Exception {
        String syncDbRequestPayload = FileUtils.getFile(this.getClass(), "requests/sync_db/sync_db.json");

        SyncDbRequestBean syncDbRequestBean = mapper.readValue(syncDbRequestPayload,
                SyncDbRequestBean.class);

        MvcResult result = this.mockUtilController.perform(
                post(urlPrepend(Constants.URL_SYNC_DB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                        .content(mapper.writeValueAsString(syncDbRequestBean)))
                .andExpect(status().isOk())
                .andReturn();

        return mapper.readValue(result.getResponse().getContentAsString(),
                SyncDbResponseBean.class);
    }

    RepeatResponseBean newRepeatRequest(String sessionId) throws Exception {

        String newRepeatRequestPayload = FileUtils.getFile(this.getClass(), "requests/new_repeat/new_repeat.json");

        RepeatRequestBean newRepeatRequestBean = mapper.readValue(newRepeatRequestPayload,
                RepeatRequestBean.class);
        newRepeatRequestBean.setSessionId(sessionId);

        String newRepeatRequestString = mapper.writeValueAsString(newRepeatRequestBean);

        String repeatResult = mockFormController.perform(
                post(urlPrepend(Constants.URL_NEW_REPEAT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(newRepeatRequestString)).andReturn().getResponse().getContentAsString();
        return mapper.readValue(repeatResult, RepeatResponseBean.class);
    }

    RepeatResponseBean deleteRepeatRequest(String sessionId) throws Exception {

        String newRepeatRequestPayload = FileUtils.getFile(this.getClass(), "requests/delete_repeat/delete_repeat.json");

        RepeatRequestBean newRepeatRequestBean = mapper.readValue(newRepeatRequestPayload,
                RepeatRequestBean.class);
        newRepeatRequestBean.setSessionId(sessionId);

        String newRepeatRequestString = mapper.writeValueAsString(newRepeatRequestBean);

        String repeatResult = mockFormController.perform(
                post(urlPrepend(Constants.URL_DELETE_REPEAT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(newRepeatRequestString)).andReturn().getResponse().getContentAsString();
        return mapper.readValue(repeatResult, RepeatResponseBean.class);
    }

    CurrentResponseBean getCurrent(String sessionId) throws Exception{
        CurrentRequestBean currentRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/current/current_request.json"), CurrentRequestBean.class);
        currentRequestBean.setSessionId(sessionId);

        ResultActions currentResult = mockFormController.perform(
                get(urlPrepend(Constants.URL_CURRENT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(currentRequestBean)));
        String currentResultString = currentResult.andReturn().getResponse().getContentAsString();
        return mapper.readValue(currentResultString, CurrentResponseBean.class);
    }

    GetInstanceResponseBean getInstance(String sessionId) throws Exception{
        GetInstanceRequestBean getInstanceRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/current/current_request.json"), GetInstanceRequestBean.class);
        getInstanceRequestBean.setSessionId(sessionId);
        ResultActions getInstanceResult = mockFormController.perform(
                post(urlPrepend(Constants.URL_GET_INSTANCE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(getInstanceRequestBean)));
        String getInstanceResultString = getInstanceResult.andReturn().getResponse().getContentAsString();
        return mapper.readValue(getInstanceResultString, GetInstanceResponseBean.class);
    }

    EvaluateXPathResponseBean evaluateXPath(String sessionId) throws Exception{
        EvaluateXPathRequestBean evaluateXPathRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/evaluate_xpath/evaluate_xpath.json"), EvaluateXPathRequestBean.class);
        evaluateXPathRequestBean.setSessionId(sessionId);
        evaluateXPathRequestBean.setXpath("/data/q_text");
        ResultActions evaluateXpathResult = mockFormController.perform(
                get(urlPrepend(Constants.URL_EVALUATE_XPATH))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(evaluateXPathRequestBean)));
        String evaluateXpathResultString = evaluateXpathResult.andReturn().getResponse().getContentAsString();
        return mapper.readValue(evaluateXpathResultString,
                EvaluateXPathResponseBean.class);
    }

    JSONObject sessionNavigate(String requestPath) throws Exception {
        SessionNavigationBean sessionNavigationBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), SessionNavigationBean.class);
        ResultActions selectResult = mockMenuController.perform(
                post(urlPrepend(Constants.URL_MENU_NAVIGATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                        .content(mapper.writeValueAsString(sessionNavigationBean)));
        String resultString = selectResult.andReturn().getResponse().getContentAsString();
        return new JSONObject(resultString);
    }

    JSONObject sessionNavigate(String[] selections, String testName) throws Exception {
        return sessionNavigate(selections, testName, null);
    }

    JSONObject sessionNavigate(String[] selections, String testName, String locale) throws Exception {
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setDomain(testName + "domain");
        sessionNavigationBean.setAppId(testName + "appid");
        sessionNavigationBean.setUsername(testName + "username");
        sessionNavigationBean.setSelections(selections);
        if(locale != null && !"".equals(locale.trim())){
            sessionNavigationBean.setLocale(locale);
        }
        ResultActions selectResult = mockMenuController.perform(
                post(urlPrepend(Constants.URL_MENU_NAVIGATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                        .content(mapper.writeValueAsString(sessionNavigationBean)));
        String resultString = selectResult.andReturn().getResponse().getContentAsString();
        return new JSONObject(resultString);
    }

    CommandListResponseBean doInstall(String requestPath) throws Exception {
        InstallRequestBean installRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), InstallRequestBean.class);
        ResultActions installResult = mockMenuController.perform(
                post(urlPrepend(Constants.URL_INSTALL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                        .content(mapper.writeValueAsString(installRequestBean)));
        String installResultString = installResult.andReturn().getResponse().getContentAsString();
        return mapper.readValue(installResultString,
                CommandListResponseBean.class);
    }
}
