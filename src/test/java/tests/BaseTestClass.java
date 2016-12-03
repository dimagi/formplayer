package tests;

import application.DebuggerController;
import application.FormController;
import application.MenuController;
import application.UtilController;
import auth.HqAuth;
import beans.*;
import beans.debugger.XPathQueryItem;
import beans.menus.CommandListResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableFormSession;
import org.apache.commons.lang3.StringUtils;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.util.engine.CommCareConfigEngine;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.junit.Before;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import repo.FormSessionRepo;
import repo.MenuSessionRepo;
import repo.SerializableMenuSession;
import services.*;
import util.Constants;
import util.PrototypeUtils;
import utils.FileUtils;
import utils.TestContext;

import javax.servlet.http.Cookie;
import java.io.File;
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
@ContextConfiguration(classes = TestContext.class)
public class BaseTestClass {

    private MockMvc mockFormController;

    private MockMvc mockUtilController;

    private MockMvc mockMenuController;

    private MockMvc mockDebuggerController;

    @Spy
    private StringRedisTemplate redisTemplate;

    @Autowired
    private FormSessionRepo formSessionRepoMock;

    @Autowired
    private MenuSessionRepo menuSessionRepoMock;

    @Autowired
    private XFormService xFormServiceMock;

    @Autowired
    RestoreFactory restoreFactoryMock;

    @Autowired
    SubmitService submitServiceMock;

    @Autowired
    private InstallService installService;

    @Autowired
    private NewFormResponseFactory newFormResponseFactoryMock;

    @Autowired
    protected LockRegistry userLockRegistry;

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

    protected ObjectMapper mapper;

    final SerializableFormSession serializableFormSession = new SerializableFormSession();
    final SerializableMenuSession serializableMenuSession = new SerializableMenuSession();

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
        MockitoAnnotations.initMocks(this);
        mockFormController = MockMvcBuilders.standaloneSetup(formController).build();
        mockUtilController = MockMvcBuilders.standaloneSetup(utilController).build();
        mockMenuController = MockMvcBuilders.standaloneSetup(menuController).build();
        mockDebuggerController = MockMvcBuilders.standaloneSetup(debuggerController).build();
        Mockito.doReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"))
                .when(restoreFactoryMock).getRestoreXml();
        when(submitServiceMock.submitForm(anyString(), anyString(), any(HqAuth.class)))
                .thenReturn(new ResponseEntity<String>(HttpStatus.OK));
        mapper = new ObjectMapper();
        setupFormSessionRepoMock();
        setupMenuSessionRepoMock();
        setupInstallServiceMock();
        setupLockMock();
        setupNewFormMock();
        PrototypeUtils.setupPrototypes();
    }

    private void setupNewFormMock() throws Exception {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                NewFormResponseFactory newFormResponseFactory = new NewFormResponseFactory(formSessionRepoMock,
                        xFormServiceMock,
                        restoreFactoryMock);
                return newFormResponseFactory.getResponse((NewSessionRequestBean)args[0], (String)args[1], (HqAuth)args[2]);
            }
        }).when(newFormResponseFactoryMock).getResponse(any(NewSessionRequestBean.class), anyString(), any(HqAuth.class));
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

    private void setupInstallServiceMock() {
        try {
            doAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    try {
                        Object[] args = invocationOnMock.getArguments();
                        String ref = (String) args[0];
                        final String username = (String) args[1];
                        final String path = (String) args[2];
                        final String trimmedUsername = StringUtils.substringBefore(username, "@");
                        File dbFolder = new File(path);
                        dbFolder.delete();
                        dbFolder.mkdirs();
                        final LivePrototypeFactory mPrototypeFactory = new LivePrototypeFactory();
                        CommCareConfigEngine.setStorageFactory(new IStorageIndexedFactory() {
                            @Override
                            public IStorageUtilityIndexed newStorage(String name, Class type) {
                                return new SqliteIndexedStorageUtility(type, name, trimmedUsername, path);
                            }
                        });
                        CommCareConfigEngine engine = new CommCareConfigEngine(mPrototypeFactory);
                        String absolutePath = getTestResourcePath(ref);
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
        System.out.println("Get test resource at path " + resourcePath);
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
        }).when(menuSessionRepoMock).findOneWrapped(anyString());

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

    protected void configureRestoreFactory(String domain, String username) {
        restoreFactoryMock.setDomain(domain);
        restoreFactoryMock.setUsername(username);
    }

    FormEntryResponseBean jumpToIndex(String index, String sessionId) throws Exception {
        JumpToIndexRequestBean questionsBean = new JumpToIndexRequestBean(index, sessionId);
        return generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_QUESTIONS_FOR_INDEX,
                questionsBean,
                FormEntryResponseBean.class);
    }

  FormEntryNavigationResponseBean nextScreen(String sessionId) throws Exception {
        SessionRequestBean questionsBean = new SessionRequestBean();
        questionsBean.setSessionId(sessionId);
        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(questionsBean);
        MvcResult answerResult = this.mockFormController.perform(
                post(urlPrepend(Constants.URL_NEXT_INDEX))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andReturn();

        return mapper.readValue(answerResult.getResponse().getContentAsString(),
            FormEntryNavigationResponseBean.class);
    }

  FormEntryNavigationResponseBean previousScreen(String sessionId) throws Exception {
        SessionRequestBean questionsBean = new SessionRequestBean();
        questionsBean.setSessionId(sessionId);
        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(questionsBean);
        MvcResult answerResult = this.mockFormController.perform(
                post(urlPrepend(Constants.URL_PREV_INDEX))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andReturn();

        return mapper.readValue(answerResult.getResponse().getContentAsString(),
            FormEntryNavigationResponseBean.class);
    }

    FormEntryResponseBean answerQuestionGetResult(String index, String answer, String sessionId) throws Exception {
        AnswerQuestionRequestBean answerQuestionBean = new AnswerQuestionRequestBean(index, answer, sessionId);
        return generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_ANSWER_QUESTION,
                answerQuestionBean,
                FormEntryResponseBean.class);
    }

    NewFormResponse startNewSession(String requestPath, String formPath) throws Exception {
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

    CaseFilterResponseBean filterCases(String requestPath) throws Exception {
        String filterRequestPayload = FileUtils.getFile(this.getClass(), requestPath);
        return generateMockQuery(ControllerType.UTIL,
                RequestType.GET,
                Constants.URL_FILTER_CASES,
                filterRequestPayload,
                CaseFilterResponseBean.class);
    }

    CaseFilterFullResponseBean filterCasesFull() throws Exception {
        String filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases.json");
        return generateMockQuery(ControllerType.UTIL,
                RequestType.GET,
                Constants.URL_FILTER_CASES_FULL,
                filterRequestPayload,
                CaseFilterFullResponseBean.class);
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

    InstanceXmlBean getInstance(String sessionId) throws Exception {
        GetInstanceRequestBean getInstanceRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), "requests/current/current_request.json"), GetInstanceRequestBean.class);
        getInstanceRequestBean.setSessionId(sessionId);
        return generateMockQuery(ControllerType.FORM,
                RequestType.POST,
                Constants.URL_GET_INSTANCE,
                getInstanceRequestBean,
                InstanceXmlBean.class);
    }

    EvaluateXPathResponseBean evaluateXPath(String sessionId, String xPath) throws Exception {
        EvaluateXPathRequestBean evaluateXPathRequestBean = mapper.readValue(
                FileUtils.getFile(this.getClass(), "requests/evaluate_xpath/evaluate_xpath.json"),
                EvaluateXPathRequestBean.class
        );
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

    <T> T sessionNavigate(String requestPath, Class<T> clazz) throws Exception {
        SessionNavigationBean sessionNavigationBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), SessionNavigationBean.class);
        return generateMockQuery(ControllerType.MENU,
                RequestType.POST,
                Constants.URL_MENU_NAVIGATION,
                sessionNavigationBean,
                clazz);
    }

    <T> T sessionNavigate(String[] selections, String testName, Class <T> clazz) throws Exception {
        return sessionNavigate(selections, testName, null, clazz);
    }

    <T> T sessionNavigate(String[] selections, String testName, String locale, Class<T> clazz) throws Exception {
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setDomain(testName + "domain");
        sessionNavigationBean.setAppId(testName + "appid");
        sessionNavigationBean.setUsername(testName + "username");
        sessionNavigationBean.setInstallReference("archives/" + testName + ".ccz");
        sessionNavigationBean.setSelections(selections);
        if(locale != null && !"".equals(locale.trim())){
            sessionNavigationBean.setLocale(locale);
        }
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
