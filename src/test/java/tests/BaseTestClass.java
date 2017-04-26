package tests;

import application.*;
import auth.DjangoAuth;
import auth.HqAuth;
import beans.*;
import beans.debugger.XPathQueryItem;
import beans.menus.CommandListResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import installers.FormplayerInstallerFactory;
import org.junit.After;
import org.junit.Before;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import repo.FormSessionRepo;
import repo.MenuSessionRepo;
import repo.SerializableMenuSession;
import sandbox.SqlSandboxUtils;
import services.*;
import util.Constants;
import util.PrototypeUtils;
import utils.FileUtils;
import utils.TestContext;

import javax.servlet.http.Cookie;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Created by willpride on 2/3/16.
 */
@ContextConfiguration(classes = TestContext.class)
public class BaseTestClass {

    private MockMvc mockFormController;

    private MockMvc mockUtilController;

    private MockMvc mockMenuController;

    private MockMvc mockDebuggerController;

    @Spy
    private StringRedisTemplate redisTemplate;

    @Autowired
    protected FormSessionRepo formSessionRepoMock;

    @Autowired
    private MenuSessionRepo menuSessionRepoMock;

    @Autowired
    private XFormService xFormServiceMock;

    @Autowired
    RestoreFactory restoreFactoryMock;

    @Autowired
    FormplayerStorageFactory storageFactoryMock;

    @Autowired
    SubmitService submitServiceMock;

    @Autowired
    private InstallService installService;

    @Autowired
    private NewFormResponseFactory newFormResponseFactoryMock;

    @Autowired
    protected LockRegistry userLockRegistry;

    @Autowired
    protected FormplayerInstallerFactory formplayerInstallerFactory;

    @Autowired
    protected QueryRequester queryRequester;

    @Autowired
    protected SyncRequester syncRequester;

    @InjectMocks
    protected FormController formController;

    @InjectMocks
    protected UtilController utilController;

    @InjectMocks
    protected MenuController menuController;

    @InjectMocks
    protected DebuggerController debuggerController;

    @Mock
    private ListOperations<String, XPathQueryItem> listOperations;

    @Mock
    private ValueOperations<String, Long> valueOperations;

    protected ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        Mockito.reset(formSessionRepoMock);
        Mockito.reset(menuSessionRepoMock);
        Mockito.reset(xFormServiceMock);
        Mockito.reset(restoreFactoryMock);
        Mockito.reset(submitServiceMock);
        Mockito.reset(installService);
        Mockito.reset(userLockRegistry);
        Mockito.reset(newFormResponseFactoryMock);
        Mockito.reset(storageFactoryMock);
        Mockito.reset(formplayerInstallerFactory);
        Mockito.reset(queryRequester);
        Mockito.reset(syncRequester);
        MockitoAnnotations.initMocks(this);
        mockFormController = MockMvcBuilders.standaloneSetup(formController).build();
        mockUtilController = MockMvcBuilders.standaloneSetup(utilController).build();
        mockMenuController = MockMvcBuilders.standaloneSetup(menuController).build();
        mockDebuggerController = MockMvcBuilders.standaloneSetup(debuggerController).build();
        RestoreFactoryAnswer answer = new RestoreFactoryAnswer(this.getMockRestoreFileName());
        Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml(anyBoolean());
        Mockito.doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(submitServiceMock).submitForm(anyString(), anyString(), any(HqAuth.class));
        Mockito.doReturn(false)
                .when(restoreFactoryMock).isRestoreXmlExpired();
        mapper = new ObjectMapper();
        storageFactoryMock.closeConnection();
        restoreFactoryMock.closeConnection();
        PrototypeUtils.setupPrototypes();
        new SQLiteProperties().setDataDir("testdbs/");
    }

    @After
    public void tearDown() {
        SqlSandboxUtils.deleteDatabaseFolder(SQLiteProperties.getDataDir());
    }

    public class RestoreFactoryAnswer implements Answer {
        private String mRestoreFile;

        public RestoreFactoryAnswer(String restoreFile) {
            mRestoreFile = restoreFile;
        }

        @Override
        public InputStream answer(InvocationOnMock invocation) throws Throwable {
            return new FileInputStream("src/test/resources/" + mRestoreFile);
        }
    }

    private String urlPrepend(String string) {
        return "/" + string;
    }

    protected String getMockRestoreFileName() {
        return "test_restore.xml";
    }

    protected void configureRestoreFactory(String domain, String username) {
        restoreFactoryMock.setDomain(domain);
        restoreFactoryMock.setUsername(username);
    }

    FormEntryNavigationResponseBean nextScreen(String sessionId) throws Exception {
        SessionRequestBean questionsBean = new SessionRequestBean();
        questionsBean.setSessionId(sessionId);
        questionsBean.setUsername(formSessionRepoMock.findOneWrapped(sessionId).getUsername());
        questionsBean.setDomain(formSessionRepoMock.findOneWrapped(sessionId).getDomain());
        return generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_NEXT_INDEX,
                questionsBean,
                FormEntryNavigationResponseBean.class);
    }

    FormEntryNavigationResponseBean previousScreen(String sessionId) throws Exception {
        SessionRequestBean questionsBean = new SessionRequestBean();
        questionsBean.setSessionId(sessionId);
        questionsBean.setUsername(formSessionRepoMock.findOneWrapped(sessionId).getUsername());
        questionsBean.setDomain(formSessionRepoMock.findOneWrapped(sessionId).getDomain());
        return generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_PREV_INDEX,
                questionsBean,
                FormEntryNavigationResponseBean.class);
    }

    FormEntryResponseBean answerQuestionGetResult(String index, String answer, String sessionId) throws Exception {
        AnswerQuestionRequestBean answerQuestionBean = new AnswerQuestionRequestBean(index, answer, sessionId);
        answerQuestionBean.setUsername(formSessionRepoMock.findOneWrapped(sessionId).getUsername());
        answerQuestionBean.setDomain(formSessionRepoMock.findOneWrapped(sessionId).getDomain());
        return generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_ANSWER_QUESTION,
                answerQuestionBean,
                FormEntryResponseBean.class);
    }

    NewFormResponse startNewForm(String requestPath, String formPath) throws Exception {
        when(xFormServiceMock.getFormXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), formPath));
        String requestPayload = FileUtils.getFile(this.getClass(), requestPath);
        NewSessionRequestBean newSessionRequestBean = mapper.readValue(requestPayload,
                NewSessionRequestBean.class);
        when(restoreFactoryMock.getUsername())
                .thenReturn(newSessionRequestBean.getUsername());
        when(restoreFactoryMock.getDomain())
                .thenReturn(newSessionRequestBean.getDomain());
        return generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_NEW_SESSION,
                newSessionRequestBean,
                NewFormResponse.class);
    }

    SubmitResponseBean submitForm(String requestPath, String sessionId) throws Exception {
        SubmitRequestBean submitRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), SubmitRequestBean.class);
        submitRequestBean.setSessionId(sessionId);
        return generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_SUBMIT_FORM,
                submitRequestBean,
                SubmitResponseBean.class);
    }

    SubmitResponseBean submitForm(Map<String, Object> answers, String sessionId) throws Exception {
        SubmitRequestBean submitRequestBean = new SubmitRequestBean();
        submitRequestBean.setSessionId(sessionId);
        submitRequestBean.setAnswers(answers);
        submitRequestBean.setPrevalidated(true);
        submitRequestBean.setUsername(formSessionRepoMock.findOneWrapped(sessionId).getUsername());
        submitRequestBean.setDomain(formSessionRepoMock.findOneWrapped(sessionId).getDomain());
        return generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_SUBMIT_FORM,
                submitRequestBean,
                SubmitResponseBean.class);
    }

    SyncDbResponseBean syncDb() throws Exception {
        String syncDbRequestPayload = FileUtils.getFile(this.getClass(), "requests/sync_db/sync_db.json");
        SyncDbRequestBean syncDbRequestBean = mapper.readValue(syncDbRequestPayload,
                SyncDbRequestBean.class);
        return generateMockQuery(ControllerType.UTIL,
                RequestType.POST,
                Constants.URL_SYNC_DB,
                syncDbRequestBean,
                SyncDbResponseBean.class);
    }

    NotificationMessageBean deleteApplicationDbs() throws Exception {
        String payload = FileUtils.getFile(this.getClass(), "requests/delete_db/delete_db.json");
        DeleteApplicationDbsRequestBean request = mapper.readValue(
                payload,
                DeleteApplicationDbsRequestBean.class
        );

        return generateMockQuery(
                ControllerType.UTIL,
                RequestType.POST,
                Constants.URL_DELETE_APPLICATION_DBS,
                request,
                NotificationMessageBean.class
        );
    }

    FormEntryResponseBean newRepeatRequest(String sessionId) throws Exception {
        String newRepeatRequestPayload = FileUtils.getFile(this.getClass(), "requests/new_repeat/new_repeat.json");
        RepeatRequestBean newRepeatRequestBean = mapper.readValue(newRepeatRequestPayload,
                RepeatRequestBean.class);
        newRepeatRequestBean.setSessionId(sessionId);
        newRepeatRequestBean.setUsername(formSessionRepoMock.findOneWrapped(sessionId).getUsername());
        newRepeatRequestBean.setDomain(formSessionRepoMock.findOneWrapped(sessionId).getDomain());

        return generateMockQuery(
                ControllerType.FORM,
                RequestType.POST,
                Constants.URL_NEW_REPEAT,
                newRepeatRequestBean,
                FormEntryResponseBean.class
        );
    }

    FormEntryResponseBean deleteRepeatRequest(String sessionId) throws Exception {

        String newRepeatRequestPayload = FileUtils.getFile(this.getClass(), "requests/delete_repeat/delete_repeat.json");

        RepeatRequestBean deleteRepeatRequest = mapper.readValue(newRepeatRequestPayload,
                RepeatRequestBean.class);
        deleteRepeatRequest.setSessionId(sessionId);
        deleteRepeatRequest.setUsername(formSessionRepoMock.findOneWrapped(sessionId).getUsername());
        deleteRepeatRequest.setDomain(formSessionRepoMock.findOneWrapped(sessionId).getDomain());
        return generateMockQuery(
                ControllerType.FORM,
                RequestType.POST,
                Constants.URL_DELETE_REPEAT,
                deleteRepeatRequest,
                FormEntryResponseBean.class
        );
    }

    EvaluateXPathResponseBean evaluateXPath(String sessionId, String xPath) throws Exception {
        EvaluateXPathRequestBean evaluateXPathRequestBean = mapper.readValue(
                FileUtils.getFile(this.getClass(), "requests/evaluate_xpath/evaluate_xpath.json"),
                EvaluateXPathRequestBean.class
        );
        evaluateXPathRequestBean.setUsername(formSessionRepoMock.findOneWrapped(sessionId).getUsername());
        evaluateXPathRequestBean.setDomain(formSessionRepoMock.findOneWrapped(sessionId).getDomain());
        evaluateXPathRequestBean.setSessionId(sessionId);
        evaluateXPathRequestBean.setXpath(xPath);
        return generateMockQuery(
                ControllerType.DEBUGGER,
                RequestType.POST,
                Constants.URL_EVALUATE_XPATH,
                evaluateXPathRequestBean,
                EvaluateXPathResponseBean.class
        );
    }

    <T> T getDetails(String requestPath, Class<T> clazz) throws Exception {
        SessionNavigationBean sessionNavigationBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), SessionNavigationBean.class);
        return generateMockQuery(ControllerType.MENU,
                RequestType.POST,
                Constants.URL_GET_DETAILS,
                sessionNavigationBean,
                clazz);
    }

    <T> T getDetails(String[] selections, String testName, Class<T> clazz) throws Exception {
        return getDetails(selections, testName, null, clazz);
    }

    <T> T getDetails(String[] selections, String testName, String locale, Class<T> clazz) throws Exception {
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setDomain(testName + "domain");
        sessionNavigationBean.setAppId(testName + "appid");
        sessionNavigationBean.setUsername(testName + "username");
        sessionNavigationBean.setInstallReference("archives/" + testName + ".ccz");
        sessionNavigationBean.setSelections(selections);
        if (locale != null && !"".equals(locale.trim())) {
            sessionNavigationBean.setLocale(locale);
        }
        return generateMockQuery(ControllerType.MENU,
                RequestType.POST,
                Constants.URL_GET_DETAILS,
                sessionNavigationBean,
                clazz);
    }

    <T> T sessionNavigate(String requestPath, Class<T> clazz) throws Exception {
        SessionNavigationBean sessionNavigationBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), SessionNavigationBean.class);
        return generateMockQuery(ControllerType.MENU,
                RequestType.POST,
                Constants.URL_MENU_NAVIGATION,
                sessionNavigationBean,
                clazz);
    }

    <T> T sessionNavigate(String[] selections, String testName, Class<T> clazz) throws Exception {
        return sessionNavigate(selections, testName, null, clazz);
    }

    <T> T sessionNavigate(String[] selections, String testName, String locale, Class<T> clazz) throws Exception {
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setDomain(testName + "domain");
        sessionNavigationBean.setAppId(testName + "appid");
        sessionNavigationBean.setUsername(testName + "username");
        sessionNavigationBean.setInstallReference("archives/" + testName + ".ccz");
        sessionNavigationBean.setSelections(selections);
        if (locale != null && !"".equals(locale.trim())) {
            sessionNavigationBean.setLocale(locale);
        }
        return generateMockQuery(ControllerType.MENU,
                RequestType.POST,
                Constants.URL_MENU_NAVIGATION,
                sessionNavigationBean,
                clazz);
    }

    <T> T sessionNavigate(String[] selections, String testName, String locale, Class<T> clazz, String restoreAs) throws Exception {
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setDomain(testName + "domain");
        sessionNavigationBean.setAppId(testName + "appid");
        sessionNavigationBean.setUsername(testName + "username");
        sessionNavigationBean.setInstallReference("archives/" + testName + ".ccz");
        sessionNavigationBean.setSelections(selections);
        sessionNavigationBean.setRestoreAs(restoreAs);
        if (locale != null && !"".equals(locale.trim())) {
            sessionNavigationBean.setLocale(locale);
        }
        return generateMockQuery(ControllerType.MENU,
                RequestType.POST,
                Constants.URL_MENU_NAVIGATION,
                sessionNavigationBean,
                clazz);
    }

    <T> T sessionNavigateWithQuery(String[] selections,
                                   String testName,
                                   Hashtable<String, String> queryDictionary,
                                   Class<T> clazz) throws Exception {
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setSelections(selections);
        sessionNavigationBean.setDomain(testName + "domain");
        sessionNavigationBean.setAppId(testName + "appid");
        sessionNavigationBean.setUsername(testName + "username");
        sessionNavigationBean.setInstallReference("archives/" + testName + ".ccz");
        sessionNavigationBean.setQueryDictionary(queryDictionary);
        return generateMockQuery(ControllerType.MENU,
                RequestType.POST,
                Constants.URL_MENU_NAVIGATION,
                sessionNavigationBean,
                clazz);
    }

    <T> T sessionNavigateWithId(String[] selections, String sessionId, Class<T> clazz) throws Exception {
        SerializableMenuSession menuSession = menuSessionRepoMock.findOneWrapped(sessionId);
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setDomain(menuSession.getDomain());
        sessionNavigationBean.setAppId(menuSession.getAppId());
        sessionNavigationBean.setUsername(menuSession.getUsername());
        sessionNavigationBean.setSelections(selections);
        sessionNavigationBean.setMenuSessionId(sessionId);
        sessionNavigationBean.setInstallReference(menuSession.getInstallReference());
        return generateMockQuery(ControllerType.MENU,
                RequestType.POST,
                Constants.URL_MENU_NAVIGATION,
                sessionNavigationBean,
                clazz);
    }

    CommandListResponseBean doInstall(String requestPath) throws Exception {
        InstallRequestBean installRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), InstallRequestBean.class);
        return generateMockQuery(ControllerType.MENU,
                RequestType.POST,
                Constants.URL_INSTALL,
                installRequestBean,
                CommandListResponseBean.class);
    }

    CommandListResponseBean doUpdate(String requestPath) throws Exception {
        InstallRequestBean installRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), InstallRequestBean.class);
        return generateMockQuery(ControllerType.MENU,
                RequestType.POST,
                Constants.URL_UPDATE,
                installRequestBean,
                CommandListResponseBean.class);
    }

    public enum RequestType {
        POST, GET
    }

    public enum ControllerType {
        FORM, MENU, UTIL, DEBUGGER,
    }

    private <T> T generateMockQuery(ControllerType controllerType,
                                    RequestType requestType,
                                    String urlPath,
                                    Object bean,
                                    Class<T> clazz) throws Exception {
        MockMvc controller = null;
        ResultActions evaluateXpathResult = null;

        if (bean instanceof AuthenticatedRequestBean) {
            restoreFactoryMock.configure((AuthenticatedRequestBean) bean, new DjangoAuth("derp"));
        }

        if (bean instanceof InstallRequestBean) {
            storageFactoryMock.configure((InstallRequestBean) bean);
        }

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
                controller = mockUtilController;
                break;
            case DEBUGGER:
                controller = mockDebuggerController;
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
        return mapper.readValue(
                evaluateXpathResult.andReturn().getResponse().getContentAsString(),
                clazz
        );
    }
}
