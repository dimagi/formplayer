package tests;

import application.FormController;
import application.MenuController;
import application.UtilController;
import auth.HqAuth;
import beans.*;
import beans.menus.BaseResponseBean;
import beans.menus.CommandListResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import install.FormplayerConfigEngine;
import objects.SerializableFormSession;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
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
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import repo.FormSessionRepo;
import repo.MenuSessionRepo;
import repo.SerializableMenuSession;
import services.InstallService;
import services.RestoreService;
import services.SubmitService;
import services.XFormService;
import util.Constants;
import utils.FileUtils;

import javax.servlet.http.Cookie;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

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
    private FormSessionRepo formSessionRepoMock;

    @Autowired
    private MenuSessionRepo menuSessionRepoMock;

    @Autowired
    private XFormService xFormServiceMock;

    @Autowired
    RestoreService restoreServiceMock;

    @Autowired
    SubmitService submitServiceMock;

    @Autowired
    private InstallService installService;

    @Autowired
    protected LockRegistry userLockRegistry;

    @InjectMocks
    protected FormController formController;

    @InjectMocks
    protected UtilController utilController;

    @InjectMocks
    protected MenuController menuController;

    protected ObjectMapper mapper;

    final SerializableFormSession serializableFormSession = new SerializableFormSession();
    final SerializableMenuSession serializableMenuSession = new SerializableMenuSession();

    @Before
    public void setUp() throws IOException {
        Mockito.reset(formSessionRepoMock);
        Mockito.reset(menuSessionRepoMock);
        Mockito.reset(xFormServiceMock);
        Mockito.reset(restoreServiceMock);
        Mockito.reset(submitServiceMock);
        Mockito.reset(installService);
        Mockito.reset(userLockRegistry);
        MockitoAnnotations.initMocks(this);
        mockFormController = MockMvcBuilders.standaloneSetup(formController).build();
        mockUtilController = MockMvcBuilders.standaloneSetup(utilController).build();
        mockMenuController = MockMvcBuilders.standaloneSetup(menuController).build();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"));
        when(submitServiceMock.submitForm(anyString(), anyString(), any(HqAuth.class)))
                .thenReturn(new ResponseEntity<String>(HttpStatus.OK));
        mapper = new ObjectMapper();
        setupFormSessionRepoMock();
        setupMenuSessionRepoMock();
        setupInstallServiceMock();
        setupLockMock();
    }

    private void setupLockMock() {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return new Lock() {
                    @Override
                    public void lock() {

                    }

                    @Override
                    public void lockInterruptibly() throws InterruptedException {

                    }

                    @Override
                    public boolean tryLock() {
                        return true;
                    }

                    @Override
                    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
                        return true;
                    }

                    @Override
                    public void unlock() {

                    }

                    @Override
                    public Condition newCondition() {
                        return null;
                    }
                };
            }
        }).when(userLockRegistry).obtain(any());
    }

    private String resolveAppId(String ref){
        String appId = "";
        URIBuilder uri;

        // Parses the URI and extracts the app_id from it
        try {
            uri = new URIBuilder(ref);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to parse url" + ref);
        }

        for (NameValuePair pair: uri.getQueryParams()) {
            if (pair.getName().equals("app_id")) {
                appId = pair.getValue();
                break;
            }
        }

        switch (appId) {
            case "doublemgmtappid":
                ref = "archives/parent_child.ccz";
                break;
            case "navigatorappid":
                ref = "archives/parent_child.ccz";
                break;
            case "caseappid":
                ref = "archives/case.ccz";
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
            case "casetilesappid":
                ref = "archives/casetiles.ccz";
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
                        // All references that start with `/` are a URL that needs to be parsed
                        // in order to the app id. Should be refactored.
                        if(ref.startsWith("/")){
                            ref = resolveAppId(ref);
                        }
                        String username = (String) args[1];
                        String path = (String) args[2];
                        FormplayerConfigEngine engine = new FormplayerConfigEngine(username, path);
                        String absolutePath = getTestResourcePath(ref);
                        System.out.println("Init with path: " + absolutePath);
                        engine.initFromArchive(absolutePath);
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

    private void setupFormSessionRepoMock() {

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return serializableFormSession;
            }
        }).when(formSessionRepoMock).findOneWrapped(anyString());

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
                serializableFormSession.setMenuSessionId(toBeSaved.getMenuSessionId());
                return null;
            }
        }).when(formSessionRepoMock).save(Matchers.any(SerializableFormSession.class));
    }

    private void setupMenuSessionRepoMock() {

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return serializableMenuSession;
            }
        }).when(menuSessionRepoMock).findOne(anyString());

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                SerializableMenuSession toBeSaved = (SerializableMenuSession) args[0];
                serializableMenuSession.setCommcareSession(toBeSaved.getCommcareSession());
                serializableMenuSession.setUsername(toBeSaved.getUsername());
                serializableMenuSession.setDomain(toBeSaved.getDomain());
                serializableMenuSession.setAppId(toBeSaved.getAppId());
                serializableMenuSession.setInstallReference(toBeSaved.getInstallReference());
                serializableMenuSession.setLocale(toBeSaved.getLocale());
                return null;
            }
        }).when(menuSessionRepoMock).save(Matchers.any(SerializableMenuSession.class));
    }

    private String urlPrepend(String string) {
        return "/" + string;
    }

    FormEntryResponseBean answerQuestionGetResult(String index, String answer, String sessionId, int sequenceId) throws Exception {
        AnswerQuestionRequestBean answerQuestionBean = new AnswerQuestionRequestBean(index, answer, sessionId);
        answerQuestionBean.setSequenceId(sequenceId);
        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(answerQuestionBean);
        MvcResult answerResult = this.mockFormController.perform(
                post(urlPrepend(Constants.URL_ANSWER_QUESTION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andReturn();

        return mapper.readValue(answerResult.getResponse().getContentAsString(),
                FormEntryResponseBean.class);
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
        serializableFormSession.setSequenceId(0);
        return mapper.readValue(responseBody, NewFormSessionResponse.class);
    }

    CaseFilterResponseBean filterCases(String requestPath) throws Exception {
        String filterRequestPayload = FileUtils.getFile(this.getClass(), requestPath);
        String result = generateMockQuery(ControllerType.UTIL,
                RequestType.GET,
                Constants.URL_FILTER_CASES,
                filterRequestPayload);
        return mapper.readValue(result, CaseFilterResponseBean.class);
    }

    CaseFilterFullResponseBean filterCasesFull() throws Exception {
        String filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases.json");
        String result = generateMockQuery(ControllerType.UTIL,
                RequestType.GET,
                Constants.URL_FILTER_CASES_FULL,
                filterRequestPayload);
        return mapper.readValue(result, CaseFilterFullResponseBean.class);
    }

    SubmitResponseBean submitForm(String requestPath, String sessionId) throws Exception {
        SubmitRequestBean submitRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), SubmitRequestBean.class);
        submitRequestBean.setSessionId(sessionId);
        String result = generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_SUBMIT_FORM,
                submitRequestBean);
        return mapper.readValue(result, SubmitResponseBean.class);
    }

    SyncDbResponseBean syncDb() throws Exception {
        String syncDbRequestPayload = FileUtils.getFile(this.getClass(), "requests/sync_db/sync_db.json");
        SyncDbRequestBean syncDbRequestBean = mapper.readValue(syncDbRequestPayload,
                SyncDbRequestBean.class);
        String syncResult = generateMockQuery(ControllerType.UTIL,
                RequestType.POST,
                Constants.URL_SYNC_DB,
                syncDbRequestBean);
        return mapper.readValue(syncResult, SyncDbResponseBean.class);
    }

    NotificationMessageBean deleteApplicationDbs() throws Exception {
        String payload = FileUtils.getFile(this.getClass(), "requests/delete_db/delete_db.json");
        DeleteApplicationDbsRequestBean request = mapper.readValue(
                payload,
                DeleteApplicationDbsRequestBean.class
        );

        String result = generateMockQuery(
                ControllerType.UTIL,
                RequestType.POST,
                Constants.URL_DELETE_APPLICATION_DBS,
                request
        );
        return mapper.readValue(result, NotificationMessageBean.class);
    }

    FormEntryResponseBean newRepeatRequest(String sessionId) throws Exception {
        String newRepeatRequestPayload = FileUtils.getFile(this.getClass(), "requests/new_repeat/new_repeat.json");
        RepeatRequestBean newRepeatRequestBean = mapper.readValue(newRepeatRequestPayload,
                RepeatRequestBean.class);
        newRepeatRequestBean.setSessionId(sessionId);

        String newRepeatRequestString = mapper.writeValueAsString(newRepeatRequestBean);

        String repeatResult = mockFormController.perform(
                post(urlPrepend(Constants.URL_NEW_REPEAT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(newRepeatRequestString)).andReturn().getResponse().getContentAsString();
        return mapper.readValue(repeatResult, FormEntryResponseBean.class);
    }

    FormEntryResponseBean deleteRepeatRequest(String sessionId) throws Exception {

        String newRepeatRequestPayload = FileUtils.getFile(this.getClass(), "requests/delete_repeat/delete_repeat.json");

        RepeatRequestBean newRepeatRequestBean = mapper.readValue(newRepeatRequestPayload,
                RepeatRequestBean.class);
        newRepeatRequestBean.setSessionId(sessionId);

        String newRepeatRequestString = mapper.writeValueAsString(newRepeatRequestBean);

        String repeatResult = mockFormController.perform(
                post(urlPrepend(Constants.URL_DELETE_REPEAT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(newRepeatRequestString)).andReturn().getResponse().getContentAsString();
        return mapper.readValue(repeatResult, FormEntryResponseBean.class);
    }

    FormEntryResponseBean getCurrent(String sessionId) throws Exception{
        CurrentRequestBean currentRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/current/current_request.json"), CurrentRequestBean.class);
        currentRequestBean.setSessionId(sessionId);

        ResultActions currentResult = mockFormController.perform(
                get(urlPrepend(Constants.URL_CURRENT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(currentRequestBean)));
        String currentResultString = currentResult.andReturn().getResponse().getContentAsString();
        return mapper.readValue(currentResultString, FormEntryResponseBean.class);
    }

    GetInstanceResponseBean getInstance(String sessionId) throws Exception {
        GetInstanceRequestBean getInstanceRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/current/current_request.json"), GetInstanceRequestBean.class);
        getInstanceRequestBean.setSessionId(sessionId);
        String getInstanceResultString = generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_GET_INSTANCE,
                getInstanceRequestBean);
        return mapper.readValue(getInstanceResultString, GetInstanceResponseBean.class);
    }

    EvaluateXPathResponseBean evaluateXPath(String sessionId, String xPath) throws Exception {
        EvaluateXPathRequestBean evaluateXPathRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/evaluate_xpath/evaluate_xpath.json"), EvaluateXPathRequestBean.class);
        evaluateXPathRequestBean.setSessionId(sessionId);
        evaluateXPathRequestBean.setXpath(xPath);
        String evaluateXpathResultString = generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_EVALUATE_XPATH,
                evaluateXPathRequestBean);
        return mapper.readValue(evaluateXpathResultString,
                EvaluateXPathResponseBean.class);
    }

    JSONObject sessionNavigate(String requestPath) throws Exception {
        SessionNavigationBean sessionNavigationBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), SessionNavigationBean.class);
        String result = generateMockQuery(ControllerType.MENU,
                RequestType.POST,
                Constants.URL_MENU_NAVIGATION,
                sessionNavigationBean);
        return new JSONObject(result);
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
        String result = generateMockQuery(ControllerType.MENU,
                RequestType.POST,
                Constants.URL_MENU_NAVIGATION,
                sessionNavigationBean);
        return new JSONObject(result);
    }

    JSONObject sessionNavigateWithId(String[] selections, String sessionId) throws Exception {
        SerializableMenuSession menuSession = menuSessionRepoMock.findOne(sessionId);
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setDomain(menuSession.getDomain());
        sessionNavigationBean.setAppId(menuSession.getAppId());
        sessionNavigationBean.setUsername(menuSession.getUsername());
        sessionNavigationBean.setSelections(selections);
        sessionNavigationBean.setMenuSessionId(sessionId);
        String result = generateMockQuery(ControllerType.MENU,
                RequestType.POST,
                Constants.URL_MENU_NAVIGATION,
                sessionNavigationBean);
        return new JSONObject(result);
    }

    CommandListResponseBean doInstall(String requestPath) throws Exception {
        InstallRequestBean installRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), InstallRequestBean.class);
        String result = generateMockQuery(ControllerType.MENU,
                RequestType.POST,
                Constants.URL_INSTALL,
                installRequestBean);
        return mapper.readValue(result,
                CommandListResponseBean.class);
    }

    public enum RequestType {
        POST, GET
    }

    public enum ControllerType {
        FORM, MENU, UTIL
    }

    private String generateMockQuery(ControllerType controllerType,
                                     RequestType requestType,
                                     String urlPath,
                                     Object bean) throws Exception {
        MockMvc controller = null;
        ResultActions evaluateXpathResult = null;
        if (!(bean instanceof String)) {
            bean = mapper.writeValueAsString(bean);
        }
        switch (controllerType) {
            case FORM:
                controller = mockFormController;
                break;
            case MENU:
                controller = mockMenuController;
                break;
            case UTIL:
                System.out.println("Setting To Util");
                controller = mockUtilController;
                break;
        }
        switch (requestType) {
            case POST:
                evaluateXpathResult = controller.perform(
                        post(urlPrepend(urlPath))
                                .contentType(MediaType.APPLICATION_JSON)
                                .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                                .content((String) bean));
                break;

            case GET:
                evaluateXpathResult = controller.perform(
                        get(urlPrepend(urlPath))
                                .contentType(MediaType.APPLICATION_JSON)
                                .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                                .content((String) bean));
                break;
        }
        return evaluateXpathResult.andReturn().getResponse().getContentAsString();
    }
}
