package org.commcare.formplayer.tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.commcare.core.interfaces.RemoteInstanceFetcher;
import org.commcare.formplayer.application.DebuggerController;
import org.commcare.formplayer.application.FormController;
import org.commcare.formplayer.application.FormSubmissionController;
import org.commcare.formplayer.application.MenuController;
import org.commcare.formplayer.application.SQLiteProperties;
import org.commcare.formplayer.application.UtilController;
import org.commcare.formplayer.auth.DjangoAuth;
import org.commcare.formplayer.beans.AnswerQuestionRequestBean;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.beans.ChangeLocaleRequestBean;
import org.commcare.formplayer.beans.DeleteApplicationDbsRequestBean;
import org.commcare.formplayer.beans.EvaluateXPathMenuRequestBean;
import org.commcare.formplayer.beans.EvaluateXPathRequestBean;
import org.commcare.formplayer.beans.EvaluateXPathResponseBean;
import org.commcare.formplayer.beans.FormEntryNavigationResponseBean;
import org.commcare.formplayer.beans.FormEntryResponseBean;
import org.commcare.formplayer.beans.GetInstanceResponseBean;
import org.commcare.formplayer.beans.InstallRequestBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.NewSessionRequestBean;
import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.beans.RepeatRequestBean;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.beans.SessionRequestBean;
import org.commcare.formplayer.beans.SubmitRequestBean;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.beans.SyncDbRequestBean;
import org.commcare.formplayer.beans.SyncDbResponseBean;
import org.commcare.formplayer.beans.debugger.XPathQueryItem;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.engine.FormplayerConfigEngine;
import org.commcare.formplayer.exceptions.FormNotFoundException;
import org.commcare.formplayer.exceptions.MenuNotFoundException;
import org.commcare.formplayer.installers.FormplayerInstallerFactory;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.objects.SerializableFormDefinition;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.objects.SerializableMenuSession;
import org.commcare.formplayer.sandbox.SqlSandboxUtils;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.services.CategoryTimingHelper;
import org.commcare.formplayer.services.FormDefinitionService;
import org.commcare.formplayer.services.FormSessionService;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.InstallService;
import org.commcare.formplayer.services.MenuSessionFactory;
import org.commcare.formplayer.services.MenuSessionRunnerService;
import org.commcare.formplayer.services.MenuSessionService;
import org.commcare.formplayer.services.NewFormResponseFactory;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.services.SubmitService;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.session.MenuSession;
import org.commcare.formplayer.sqlitedb.UserDB;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerDatadog;
import org.commcare.formplayer.util.NotificationLogger;
import org.commcare.formplayer.util.PrototypeUtils;
import org.commcare.formplayer.util.SessionUtils;
import org.commcare.formplayer.util.serializer.FormDefStringSerializer;
import org.commcare.formplayer.util.serializer.SessionSerializer;
import org.commcare.formplayer.utils.CheckedSupplier;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.web.client.WebClient;
import org.commcare.modern.util.Pair;
import org.commcare.session.CommCareSession;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.model.utils.TimezoneProvider;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.LocalizerManager;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Cookie;

/**
 * Created by willpride on 2/3/16.
 */
@ContextConfiguration(classes = TestContext.class)
public class BaseTestClass {

    private MockMvc mockFormController;

    private MockMvc mockFormSubmissionController;

    protected MockMvc mockUtilController;

    private MockMvc mockMenuController;

    private MockMvc mockDebuggerController;

    @Spy
    private StringRedisTemplate redisTemplate;

    @Autowired
    protected FormSessionService formSessionService;

    @Autowired
    protected FormDefinitionService formDefinitionService;

    @Autowired
    private MenuSessionService menuSessionService;

    @Autowired
    protected WebClient webClientMock;

    @Autowired
    RestoreFactory restoreFactoryMock;

    @Autowired
    FormplayerStorageFactory storageFactoryMock;

    @Autowired
    SubmitService submitServiceMock;

    @Autowired
    CategoryTimingHelper categoryTimingHelper;

    @Autowired
    private InstallService installService;

    @Autowired
    private NewFormResponseFactory newFormResponseFactoryMock;

    @Autowired
    protected FormplayerDatadog datadogMock;

    @Autowired
    protected LockRegistry userLockRegistry;

    @Autowired
    protected FormplayerInstallerFactory formplayerInstallerFactory;

    @Autowired
    protected MenuSessionFactory menuSessionFactory;

    @Autowired
    protected MenuSessionRunnerService menuSessionRunnerService;

    @Autowired
    private FormSendCalloutHandler formSendCalloutHandlerMock;

    @Autowired
    private NotificationLogger notificationLogger;

    @InjectMocks
    protected FormController formController;

    @InjectMocks
    protected FormSubmissionController formSubmissionController;

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

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SetOperations<String, String> redisSessionCache;

    @Mock
    protected RemoteInstanceFetcher remoteInstanceFetcherMock;

    protected ObjectMapper mapper;

    final Map<String, SerializableFormSession> sessionMap = new HashMap<>();
    final Map<Long, SerializableFormDefinition> formDefinitionMap = new HashMap<>();
    final Map<String, SerializableMenuSession> menuSessionMap = new HashMap<>();

    protected Long currentFormDefinitionId = 1L;

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.reset(webClientMock);
        Mockito.reset(restoreFactoryMock);
        Mockito.reset(submitServiceMock);
        Mockito.reset(categoryTimingHelper);
        Mockito.reset(installService);
        Mockito.reset(userLockRegistry);
        Mockito.reset(newFormResponseFactoryMock);
        Mockito.reset(storageFactoryMock);
        Mockito.reset(formplayerInstallerFactory);
        Mockito.reset(datadogMock);
        Mockito.reset(menuSessionFactory);
        Mockito.reset(menuSessionRunnerService);
        Mockito.reset(remoteInstanceFetcherMock);
        Mockito.reset(notificationLogger);
        MockitoAnnotations.openMocks(this);
        mockFormController = MockMvcBuilders.standaloneSetup(formController).build();
        mockFormSubmissionController = MockMvcBuilders.standaloneSetup(
                formSubmissionController).build();
        mockUtilController = MockMvcBuilders.standaloneSetup(utilController).build();
        mockMenuController = MockMvcBuilders.standaloneSetup(menuController).build();
        mockDebuggerController = MockMvcBuilders.standaloneSetup(debuggerController).build();
        RestoreFactoryAnswer answer = new RestoreFactoryAnswer(this.getMockRestoreFileName());
        doAnswer(answer).when(restoreFactoryMock).getRestoreXml(anyBoolean());
        setupSubmitServiceMock();
        Mockito.doReturn(false)
                .when(restoreFactoryMock).isRestoreXmlExpired();
        mapper = new ObjectMapper();
        storageFactoryMock.getSQLiteDB().closeConnection();
        restoreFactoryMock.getSQLiteDB().closeConnection();
        PrototypeUtils.setupThreadLocalPrototypes();
        LocalizerManager.setUseThreadLocalStrategy(true);
        LocalizerManager.getGlobalLocalizer().addAvailableLocale("default");
        Localization.setLocale("default");
        new SQLiteProperties().setDataDir(getDatabaseFolderRoot());
        MockTimezoneProvider tzProvider = new MockTimezoneProvider();
        DateUtils.setTimezoneProvider(tzProvider);

        mockFormSessionService();
        mockFormDefinitionService();
        mockMenuSessionService();
    }

    /*
     * Setup mocking for the FormSessionService that allows saving and retrieving sessions.
     * The 'persisted' sessions are cleared at the start of each test.
     */
    private void mockFormSessionService() {
        sessionMap.clear();
        doAnswer(new Answer<SerializableFormSession>() {
            @Override
            public SerializableFormSession answer(InvocationOnMock invocation) throws Throwable {
                SerializableFormSession session =
                        (SerializableFormSession)invocation.getArguments()[0];
                if (session.getId() == null) {
                    // this is normally taken care of by Hibernate
                    ReflectionTestUtils.setField(session, "id", UUID.randomUUID().toString());
                }
                sessionMap.put(session.getId(), session);
                return session;
            }
        }).when(formSessionService).saveSession(any(SerializableFormSession.class));

        when(formSessionService.getSessionById(anyString())).thenAnswer(
                new Answer<SerializableFormSession>() {
                    @Override
                    public SerializableFormSession answer(InvocationOnMock invocation)
                            throws Throwable {
                        String key = (String)invocation.getArguments()[0];
                        if (sessionMap.containsKey(key)) {
                            return sessionMap.get(key);
                        }
                        throw new FormNotFoundException(key);
                    }
                });

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                String key = (String)invocation.getArguments()[0];
                sessionMap.remove(key);
                return null;
            }
        }).when(formSessionService).deleteSessionById(anyString());
    }

    /*
     * Setup mocking for the FormDefinitionService that allows saving and retrieving form definitions.
     * The 'persisted' definitions are cleared at the start of each test.
     */
    private void mockFormDefinitionService() {
        formDefinitionMap.clear();
        currentFormDefinitionId = 1L;
        doAnswer(new Answer<SerializableFormDefinition>() {
            @Override
            public SerializableFormDefinition answer(InvocationOnMock invocation) throws Throwable {
                String appId = ((String)invocation.getArguments()[0]);
                String appVersion = ((String)invocation.getArguments()[1]);
                String xmlns = ((String)invocation.getArguments()[2]);
                for (SerializableFormDefinition tmp : formDefinitionMap.values()) {
                    if (tmp.getAppId().equals(appId) && tmp.getFormXmlns().equals(xmlns)
                            && tmp.getFormVersion().equals(appVersion)) {
                        return tmp;
                    }
                }
                // else create a new one
                String serializedFormDef;
                try {
                    serializedFormDef = FormDefStringSerializer.serialize(((FormDef)invocation.getArguments()[3]));
                } catch (IOException ex) {
                    serializedFormDef = "could not serialize provided form def";
                }
                SerializableFormDefinition serializableFormDef = new SerializableFormDefinition(
                        appId, appVersion, xmlns, serializedFormDef
                );
                if (serializableFormDef.getId() == null) {
                    // this is normally taken care of by Hibernate
                    ReflectionTestUtils.setField(serializableFormDef, "id", currentFormDefinitionId);
                    currentFormDefinitionId++;
                }
                formDefinitionMap.put(serializableFormDef.getId(), serializableFormDef);
                return serializableFormDef;
            }
        }).when(this.formDefinitionService).getOrCreateFormDefinition(
                anyString(), anyString(), anyString(), any(FormDef.class)
        );

        when(this.formDefinitionService.getFormDef(any(SerializableFormSession.class))).thenCallRealMethod();
    }

    private void mockMenuSessionService() {
        menuSessionMap.clear();
        doAnswer(new Answer<SerializableMenuSession>() {
            @Override
            public SerializableMenuSession answer(InvocationOnMock invocation) throws Throwable {
                SerializableMenuSession session =
                        (SerializableMenuSession)invocation.getArguments()[0];
                if (session.getId() == null) {
                    // this is normally taken care of by Hibernate
                    ReflectionTestUtils.setField(session, "id", UUID.randomUUID().toString());
                }
                menuSessionMap.put(session.getId(), session);
                return session;
            }
        }).when(menuSessionService).saveSession(any(SerializableMenuSession.class));

        when(menuSessionService.getSessionById(anyString())).thenAnswer(
                new Answer<SerializableMenuSession>() {
                    @Override
                    public SerializableMenuSession answer(InvocationOnMock invocation)
                            throws Throwable {
                        String key = (String)invocation.getArguments()[0];
                        if (menuSessionMap.containsKey(key)) {
                            return menuSessionMap.get(key);
                        }
                        throw new MenuNotFoundException(key);
                    }
                });
    }

    protected String getDatabaseFolderRoot() {
        return "testdbs/";
    }

    protected boolean removeDatabaseFoldersAfterTests() {
        return true;
    }

    private void setupSubmitServiceMock() {
        Mockito.doReturn(
                "<OpenRosaResponse>" +
                        "<message nature='status'>" +
                        "OK" +
                        "</message>" +
                        "</OpenRosaResponse>")
                .when(submitServiceMock).submitForm(anyString(), anyString());
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (customConnector != null) {
            customConnector.closeConnection();
        }

        if (sandbox != null) {
            sandbox.getConnection().close();
        }
        restoreFactoryMock.getSQLiteDB().closeConnection();
        storageFactoryMock.getSQLiteDB().closeConnection();
        if (removeDatabaseFoldersAfterTests()) {
            SqlSandboxUtils.deleteDatabaseFolder(SQLiteProperties.getDataDir());
        }
    }

    private UserDB customConnector;

    protected UserDB getUserDbConnector(String domain, String username, String restoreAs) {
        customConnector = new UserDB(domain, username, restoreAs);
        return customConnector;
    }

    UserSqlSandbox sandbox;

    protected UserSqlSandbox getRestoreSandbox() {
        if (sandbox != null) {
            try {
                sandbox.getConnection().close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            sandbox = null;
        }
        sandbox = restoreFactoryMock.getSqlSandbox();
        return sandbox;
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

    private <T extends SessionRequestBean> T populateFromSession(T bean, String sessionId) {
        SerializableFormSession session = formSessionService.getSessionById(sessionId);
        bean.setUsername(session.getUsername());
        bean.setDomain(session.getDomain());
        bean.setSessionId(sessionId);
        return bean;
    }

    FormEntryNavigationResponseBean nextScreen(String sessionId) throws Exception {
        return nextScreen(sessionId, false);
    }

    FormEntryNavigationResponseBean nextScreen(String sessionId, boolean promptMode)
            throws Exception {
        SessionRequestBean questionsBean = populateFromSession(new SessionRequestBean(), sessionId);
        questionsBean.setSessionId(sessionId);


        if (promptMode) {
            return generateMockQuery(ControllerType.FORM,
                    RequestType.POST,
                    Constants.URL_NEXT,
                    questionsBean,
                    FormEntryNavigationResponseBean.class);
        }

        return generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_NEXT_INDEX,
                questionsBean,
                FormEntryNavigationResponseBean.class);
    }

    FormEntryNavigationResponseBean previousScreen(String sessionId) throws Exception {
        SessionRequestBean questionsBean = populateFromSession(new SessionRequestBean(), sessionId);
        return generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_PREV_INDEX,
                questionsBean,
                FormEntryNavigationResponseBean.class);
    }

    FormEntryResponseBean changeLanguage(String locale, String sessionId) throws Exception {
        ChangeLocaleRequestBean changeLocaleBean = populateFromSession(
                new ChangeLocaleRequestBean(), sessionId);
        changeLocaleBean.setLocale(locale);
        return generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_CHANGE_LANGUAGE,
                changeLocaleBean,
                FormEntryResponseBean.class);
    }

    FormEntryResponseBean answerQuestionGetResult(String requestPath, String sessionId)
            throws Exception {
        String requestPayload = FileUtils.getFile(this.getClass(), requestPath);
        AnswerQuestionRequestBean answerQuestionBean = mapper.readValue(requestPayload,
                AnswerQuestionRequestBean.class);
        answerQuestionBean.setSessionId(sessionId);
        return generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_ANSWER_QUESTION,
                answerQuestionBean,
                FormEntryResponseBean.class);
    }

    FormEntryResponseBean answerQuestionGetResult(String index, String answer, String sessionId)
            throws Exception {
        AnswerQuestionRequestBean answerQuestionBean = new AnswerQuestionRequestBean(index, answer,
                sessionId);
        populateFromSession(answerQuestionBean, sessionId);
        return generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_ANSWER_QUESTION,
                answerQuestionBean,
                FormEntryResponseBean.class);
    }

    GetInstanceResponseBean getInstance(String sessionId) throws Exception {
        SessionRequestBean sessionRequestBean = populateFromSession(new SessionRequestBean(),
                sessionId);
        return generateMockQuery(ControllerType.FORM,
                RequestType.GET,
                Constants.URL_GET_INSTANCE,
                sessionRequestBean,
                GetInstanceResponseBean.class);
    }

    NewFormResponse startNewForm(String requestPath, String formPath) throws Exception {
        when(webClientMock.get(anyString()))
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

    SubmitResponseBean submitForm(String sessionId) throws Exception {
        return submitForm(new HashMap<String, Object>(), sessionId);
    }

    SubmitResponseBean submitForm(String requestPath, String sessionId) throws Exception {
        SubmitRequestBean submitRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), SubmitRequestBean.class);
        submitRequestBean.setSessionId(sessionId);
        return generateMockQuery(ControllerType.FORM_SUBMISSION,
                RequestType.POST,
                Constants.URL_SUBMIT_FORM,
                submitRequestBean,
                SubmitResponseBean.class);
    }


    SubmitResponseBean submitForm(Map<String, Object> answers, String sessionId) throws Exception {
        return submitForm(answers, sessionId, true);
    }

    SubmitResponseBean submitForm(Map<String, Object> answers, String sessionId,
            boolean prevalidated) throws Exception {
        SubmitRequestBean submitRequestBean = populateFromSession(new SubmitRequestBean(),
                sessionId);
        submitRequestBean.setAnswers(answers);
        submitRequestBean.setPrevalidated(prevalidated);
        return generateMockQuery(ControllerType.FORM_SUBMISSION,
                RequestType.POST,
                Constants.URL_SUBMIT_FORM,
                submitRequestBean,
                SubmitResponseBean.class);
    }

    protected SyncDbResponseBean syncDb() throws Exception {
        SyncDbRequestBean syncDbRequestBean = new SyncDbRequestBean();
        syncDbRequestBean.setDomain(restoreFactoryMock.getDomain());
        syncDbRequestBean.setUsername(restoreFactoryMock.getUsername());
        syncDbRequestBean.setRestoreAs(restoreFactoryMock.getAsUsername());
        return generateMockQuery(ControllerType.UTIL,
                RequestType.POST,
                Constants.URL_SYNC_DB,
                syncDbRequestBean,
                SyncDbResponseBean.class);
    }

    NotificationMessage deleteApplicationDbs() throws Exception {
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
                NotificationMessage.class
        );
    }

    FormEntryResponseBean newRepeatRequest(String sessionId) throws Exception {
        String newRepeatRequestPayload = FileUtils.getFile(this.getClass(),
                "requests/new_repeat/new_repeat.json");
        RepeatRequestBean newRepeatRequestBean = mapper.readValue(newRepeatRequestPayload,
                RepeatRequestBean.class);
        populateFromSession(newRepeatRequestBean, sessionId);

        return generateMockQuery(
                ControllerType.FORM,
                RequestType.POST,
                Constants.URL_NEW_REPEAT,
                newRepeatRequestBean,
                FormEntryResponseBean.class
        );
    }

    FormEntryResponseBean deleteRepeatRequest(String sessionId) throws Exception {

        String newRepeatRequestPayload = FileUtils.getFile(this.getClass(),
                "requests/delete_repeat/delete_repeat.json");

        RepeatRequestBean deleteRepeatRequest = mapper.readValue(newRepeatRequestPayload,
                RepeatRequestBean.class);
        populateFromSession(deleteRepeatRequest, sessionId);
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
        populateFromSession(evaluateXPathRequestBean, sessionId);
        evaluateXPathRequestBean.setSessionId(sessionId);
        evaluateXPathRequestBean.setXpath(xPath);
        evaluateXPathRequestBean.setDebugOutputLevel(Constants.BASIC_NO_TRACE);
        return generateMockQuery(
                ControllerType.DEBUGGER,
                RequestType.POST,
                Constants.URL_EVALUATE_XPATH,
                evaluateXPathRequestBean,
                EvaluateXPathResponseBean.class
        );
    }

    EvaluateXPathResponseBean evaluateMenuXpath(String requestPath) throws Exception {
        Pair<String, EvaluateXPathMenuRequestBean> refAndBean = getInstallReferenceAndBean(
                requestPath, EvaluateXPathMenuRequestBean.class);
        return generateMockQueryWithInstallReference(refAndBean.first,
                ControllerType.DEBUGGER,
                RequestType.POST,
                Constants.URL_EVALUATE_MENU_XPATH,
                refAndBean.second,
                EvaluateXPathResponseBean.class
        );
    }

    EvaluateXPathResponseBean evaluateMenuXpath(String menuSessionId, String xpath)
            throws Exception {
        SerializableMenuSession menuSession = menuSessionService.getSessionById(menuSessionId);

        EvaluateXPathMenuRequestBean evaluateXPathRequestBean = new EvaluateXPathMenuRequestBean();
        evaluateXPathRequestBean.setUsername(menuSession.getUsername());
        evaluateXPathRequestBean.setDomain(menuSession.getDomain());
        evaluateXPathRequestBean.setRestoreAs(menuSession.getAsUser());
        evaluateXPathRequestBean.setMenuSessionId(menuSessionId);
        evaluateXPathRequestBean.setXpath(xpath);
        return generateMockQuery(
                ControllerType.DEBUGGER,
                RequestType.POST,
                Constants.URL_EVALUATE_MENU_XPATH,
                evaluateXPathRequestBean,
                EvaluateXPathResponseBean.class
        );
    }

    <T> T getDetails(String requestPath, Class<T> clazz) throws Exception {
        Pair<String, SessionNavigationBean> refAndBean = getInstallReferenceAndBean(requestPath,
                SessionNavigationBean.class);
        return generateMockQueryWithInstallReference(refAndBean.first,
                ControllerType.MENU,
                RequestType.POST,
                Constants.URL_GET_DETAILS,
                refAndBean.second,
                clazz);
    }

    <T> T getDetails(String[] selections, String testName, Class<T> clazz) throws Exception {
        return getDetails(selections, testName, null, clazz, false);
    }

    <T> T getDetails(String[] selections, String testName, String locale, Class<T> clazz,
            boolean inline) throws Exception {
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setDomain(testName + "domain");
        sessionNavigationBean.setAppId(testName + "appid");
        sessionNavigationBean.setUsername(testName + "username");
        sessionNavigationBean.setSelections(selections);
        sessionNavigationBean.setIsPersistent(inline);
        if (locale != null && !"".equals(locale.trim())) {
            sessionNavigationBean.setLocale(locale);
        }
        return generateMockQueryWithInstallReference("archives/" + testName + ".ccz",
                ControllerType.MENU,
                RequestType.POST,
                Constants.URL_GET_DETAILS,
                sessionNavigationBean,
                clazz);
    }

    <T> T getDetailsInline(String[] selections, String testName, Class<T> clazz) throws Exception {
        return getDetails(selections, testName, null, clazz, true);
    }

    <T> T sessionNavigate(String requestPath, Class<T> clazz) throws Exception {
        Pair<String, SessionNavigationBean> refAndBean = getInstallReferenceAndBean(requestPath,
                SessionNavigationBean.class);
        return generateMockQueryWithInstallReference(refAndBean.first,
                ControllerType.MENU,
                RequestType.POST,
                Constants.URL_MENU_NAVIGATION,
                refAndBean.second,
                clazz);
    }

    <T> T sessionNavigate(String[] selections, String testName, Class<T> clazz) throws Exception {
        return sessionNavigate(selections, testName, null, clazz);
    }

    <T> T sessionNavigate(String[] selections, String testName, String locale, Class<T> clazz)
            throws Exception {
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setDomain(testName + "domain");
        sessionNavigationBean.setAppId(testName + "appid");
        sessionNavigationBean.setUsername(testName + "username");
        sessionNavigationBean.setSelections(selections);
        if (locale != null && !"".equals(locale.trim())) {
            sessionNavigationBean.setLocale(locale);
        }
        return generateMockQueryWithInstallReference("archives/" + testName + ".ccz",
                ControllerType.MENU,
                RequestType.POST,
                Constants.URL_MENU_NAVIGATION,
                sessionNavigationBean,
                clazz);
    }

    <T> T sessionNavigate(String[] selections, String testName, int sortIndex, Class<T> clazz)
            throws Exception {
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setDomain(testName + "domain");
        sessionNavigationBean.setAppId(testName + "appid");
        sessionNavigationBean.setUsername(testName + "username");
        sessionNavigationBean.setSelections(selections);
        sessionNavigationBean.setSortIndex(sortIndex);
        return generateMockQueryWithInstallReference("archives/" + testName + ".ccz",
                ControllerType.MENU,
                RequestType.POST,
                Constants.URL_MENU_NAVIGATION,
                sessionNavigationBean,
                clazz);
    }

    <T> T sessionNavigate(String[] selections, String testName, String locale, Class<T> clazz,
            String restoreAs) throws Exception {
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setDomain(testName + "domain");
        sessionNavigationBean.setAppId(testName + "appid");
        sessionNavigationBean.setUsername(testName + "username");
        sessionNavigationBean.setSelections(selections);
        sessionNavigationBean.setRestoreAs(restoreAs);
        if (locale != null && !"".equals(locale.trim())) {
            sessionNavigationBean.setLocale(locale);
        }
        return generateMockQueryWithInstallReference("archives/" + testName + ".ccz",
                ControllerType.MENU,
                RequestType.POST,
                Constants.URL_MENU_NAVIGATION,
                sessionNavigationBean,
                clazz);
    }

    <T> T sessionNavigateWithEndpoint(String testName,
            String endpointId,
            HashMap<String, String> endpointArgs,
            Class<T> clazz) throws Exception {
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setEndpointId(endpointId);
        if (endpointArgs != null) {
            sessionNavigationBean.setEndpointArgs(endpointArgs);
        }
        sessionNavigationBean.setDomain(testName + "domain");
        sessionNavigationBean.setAppId(testName + "appid");
        sessionNavigationBean.setUsername(testName + "username");
        return generateMockQueryWithInstallReference("archives/" + testName + ".ccz",
                ControllerType.MENU,
                RequestType.POST,
                Constants.URL_GET_ENDPOINT,
                sessionNavigationBean,
                clazz);
    }

    <T> T sessionNavigateWithQuery(ArrayList<String> selections,
            String testName,
            QueryData queryData,
            Class<T> clazz) throws Exception {
        return sessionNavigateWithQuery(selections.toArray(new String[selections.size()]),
                testName, queryData, clazz);
    }

    <T> T sessionNavigateWithQuery(String[] selections,
            String testName,
            QueryData queryData,
            Class<T> clazz) throws Exception {
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setSelections(selections);
        sessionNavigationBean.setDomain(testName + "domain");
        sessionNavigationBean.setAppId(testName + "appid");
        sessionNavigationBean.setUsername(testName + "username");
        sessionNavigationBean.setQueryData(queryData);
        return generateMockQueryWithInstallReference("archives/" + testName + ".ccz",
                ControllerType.MENU,
                RequestType.POST,
                Constants.URL_MENU_NAVIGATION,
                sessionNavigationBean,
                clazz);
    }

    /**
     * This function performs an app install outside of the request cycle. In order to do that
     * successfully it mimics behaviour in the UserRestoreAspect and the AppInstallAspect.
     *
     * @param requestPath path to JSON file with InstallRequestBean content as well as
     *                    'installReference'
     */
    protected CommandListResponseBean doInstall(String requestPath) throws Exception {
        Pair<String, InstallRequestBean> refAndBean = getInstallReferenceAndBean(requestPath,
                InstallRequestBean.class);
        InstallRequestBean bean = refAndBean.second;
        storageFactoryMock.configure(bean);
        restoreFactoryMock.configure(bean, new DjangoAuth("key"));
        if (bean.isMustRestore()) {
            restoreFactoryMock.performTimedSync();
        }
        CheckedSupplier<CommandListResponseBean> install = () -> {
            MenuSession menuSession = menuSessionFactory.buildSession(
                    bean.getUsername(),
                    bean.getDomain(),
                    bean.getAppId(),
                    bean.getLocale(),
                    bean.getOneQuestionPerScreen(),
                    bean.getRestoreAs(),
                    bean.getPreview()
            );
            return (CommandListResponseBean)menuSessionRunnerService.getNextMenu(menuSession);
        };
        return mockInstallReference(install, refAndBean.first);
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
        FORM, FORM_SUBMISSION, MENU, UTIL, DEBUGGER,
    }

    private <T> T mockInstallReference(CheckedSupplier<T> supplier, String installReference)
            throws Exception {
        Answer answer = invocation -> {
            Method method = invocation.getMethod();
            // only mock the `resolveInstallReference` method
            if ("resolveInstallReference".equals(method.getName())) {
                return installReference;
            } else {
                return invocation.callRealMethod();
            }
        };
        try (MockedStatic<SessionUtils> mockUtils = Mockito.mockStatic(SessionUtils.class,
                answer)) {
            return supplier.get();
        }
    }

    private <T> T generateMockQueryWithInstallReference(String installReference,
            ControllerType controllerType,
            RequestType requestType,
            String urlPath,
            Object bean,
            Class<T> clazz) throws Exception {
        return mockInstallReference(
                () -> generateMockQuery(controllerType, requestType, urlPath, bean, clazz),
                installReference
        );
    }

    private <T> T generateMockQuery(ControllerType controllerType,
            RequestType requestType,
            String urlPath,
            Object bean,
            Class<T> clazz) throws Exception {
        MockMvc controller = null;
        ResultActions result = null;

        if (bean instanceof AuthenticatedRequestBean) {
            restoreFactoryMock.getSQLiteDB().closeConnection();
            restoreFactoryMock.configure((AuthenticatedRequestBean)bean, new DjangoAuth("derp"));
        }

        if (bean instanceof InstallRequestBean) {
            storageFactoryMock.getSQLiteDB().closeConnection();
            storageFactoryMock.configure((InstallRequestBean)bean);
        }

        if (bean instanceof SessionRequestBean) {
            storageFactoryMock.getSQLiteDB().closeConnection();
            storageFactoryMock.configure(((SessionRequestBean)bean).getSessionId());
        }

        if (!(bean instanceof String)) {
            bean = mapper.writeValueAsString(bean);
        }
        switch (controllerType) {
            case FORM:
                controller = mockFormController;
                break;
            case FORM_SUBMISSION:
                controller = mockFormSubmissionController;
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
                result = controller.perform(
                        post(urlPrepend(urlPath))
                                .contentType(MediaType.APPLICATION_JSON)
                                .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                                .content((String)bean));
                break;

            case GET:
                result = controller.perform(
                        get(urlPrepend(urlPath))
                                .contentType(MediaType.APPLICATION_JSON)
                                .cookie(new Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                                .content((String)bean));
                break;
        }
        restoreFactoryMock.getSQLiteDB().closeConnection();
        storageFactoryMock.getSQLiteDB().closeConnection();
        return mapper.readValue(
                result.andReturn().getResponse().getContentAsString(),
                clazz
        );
    }

    <T> T getNextScreenForEofNavigation(SubmitResponseBean submitResponse, Class<T> clazz)
            throws IOException {
        LinkedHashMap commandsRaw = (LinkedHashMap)submitResponse.getNextScreen();
        String jsonString = new JSONObject(commandsRaw).toString();
        return mapper.readValue(jsonString, clazz);
    }

    public class MockTimezoneProvider extends TimezoneProvider {

        private int offsetMillis;

        public void setOffset(int offset) {
            this.offsetMillis = offset;
        }

        @Override
        public int getTimezoneOffsetMillis() {
            return offsetMillis;
        }
    }

    /**
     * Utility method to extract the 'installReference' field from test JSON files and map the
     * remaining JSON fields to the given bean type.
     *
     * @param requestPath classpath relative path to JSON file
     * @param clazz       Bean class map JSON to
     * @return Pair of 'installReference' and deserialized JSON bean
     * @throws IOException
     */
    private <T> Pair<String, T> getInstallReferenceAndBean(String requestPath, Class<T> clazz)
            throws IOException {
        JsonNode root = mapper.readTree(
                new StringReader(FileUtils.getFile(this.getClass(), requestPath)));
        String installReference = ((ObjectNode)root).remove("installReference").asText();
        String beanContent = mapper.writeValueAsString(root);
        return new Pair(installReference, mapper.readValue(beanContent, clazz));
    }

    protected FormSession getFormSession(SerializableFormSession serializableFormSession)
            throws Exception {
        return new FormSession(serializableFormSession,
                restoreFactoryMock,
                formSendCalloutHandlerMock,
                storageFactoryMock,
                getCommCareSession(serializableFormSession.getMenuSessionId()),
                menuSessionRunnerService.getCaseSearchHelper(),
                formDefinitionService);
    }

    @Nullable
    protected CommCareSession getCommCareSession(String menuSessionId) throws Exception {
        if (menuSessionId == null) {
            return null;
        }

        SerializableMenuSession serializableMenuSession = menuSessionService.getSessionById(
                menuSessionId);
        FormplayerConfigEngine engine = installService.configureApplication(
                serializableMenuSession.getInstallReference(),
                serializableMenuSession.isPreview()).first;
        return SessionSerializer.deserialize(engine.getPlatform(),
                serializableMenuSession.getCommcareSession());
    }
}
