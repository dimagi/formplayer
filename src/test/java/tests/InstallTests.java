package tests;

import application.SessionController;
import auth.HqAuth;
import beans.InstallRequestBean;
import beans.MenuResponseBean;
import beans.MenuSelectBean;
import beans.NewSessionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableMenuSession;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import repo.MenuRepo;
import repo.SessionRepo;
import services.RestoreService;
import services.XFormService;
import util.Constants;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Created by willpride on 1/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class InstallTests {

    protected MockMvc mockMvc;

    @Autowired
    protected MenuRepo menuRepoMock;

    @Autowired
    protected SessionRepo sessionRepoMock;

    @Autowired
    protected XFormService xFormServiceMock;

    @Autowired
    protected RestoreService restoreServiceMock;

    @InjectMocks
    SessionController sessionController;

    ObjectMapper mapper;

    final protected SerializableMenuSession serializableMenuSession = new SerializableMenuSession();

    private String urlPrepend(String string){
        return "/" + string;
    }

    @Before
    public void setUp() throws IOException {
        Mockito.reset(sessionRepoMock);
        Mockito.reset(xFormServiceMock);
        Mockito.reset(restoreServiceMock);
        Mockito.reset(menuRepoMock);
        MockitoAnnotations.initMocks(this);
        mapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(sessionController).build();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"));
        setupMenuMock();
    }

    private void setupMenuMock() {
        when(menuRepoMock.find(anyString())).thenReturn(serializableMenuSession);
        ArgumentCaptor<SerializableMenuSession> argumentCaptor = ArgumentCaptor.forClass(SerializableMenuSession.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                SerializableMenuSession toBeSaved = (SerializableMenuSession) args[0];
                serializableMenuSession.setActions(toBeSaved.getActions());
                serializableMenuSession.setUsername(toBeSaved.getUsername());
                serializableMenuSession.setDomain(toBeSaved.getDomain());
                serializableMenuSession.setActions(toBeSaved.getActions());
                serializableMenuSession.setSessionId(toBeSaved.getSessionId());
                serializableMenuSession.setInstallReference(toBeSaved.getInstallReference());
                serializableMenuSession.setPassword(toBeSaved.getPassword());
                serializableMenuSession.setSerializedCommCareSession(toBeSaved.getSerializedCommCareSession());
                serializableMenuSession.setCurrentSelection(toBeSaved.getCurrentSelection());
                System.out.println("Serializable menu session: " + serializableMenuSession);
                System.out.println("Tobesaved: " + toBeSaved);
                return null;
            }
        }).when(menuRepoMock).save(any(SerializableMenuSession.class));
    }

    @Test
    public void testNewForm() throws Exception {
        // setup files
        MenuResponseBean menuResponseBean =
                doInstall("requests/install/install.json");
        assert menuResponseBean.getOptions().size() == 12;
        assert menuResponseBean.getMenuType().equals(Constants.MENU_MODULE);
        assert menuResponseBean.getOptions().get(0).equals("Basic Form Tests");
        String sessionId = menuResponseBean.getSessionId();

        System.out.println("Session ID: " + sessionId);

        JSONObject menuResponseObject =
                selectMenu("requests/menu/menu_select.json", sessionId);


        System.out.println("Menu Response Bean 1: " + menuResponseObject);

        JSONObject menuResponseObject2 =
               selectMenu("requests/menu/menu_select.json", sessionId);

        System.out.println("Menu Response Bean 2: " + menuResponseObject2);

        assert menuResponseObject2.has("tree");
        assert menuResponseObject2.has("title");

    }


    @Test
    public void testCaseCreate() throws Exception {
        // setup files
        MenuResponseBean menuResponseBean =
                doInstall("requests/install/install.json");
        assert menuResponseBean.getOptions().size() == 12;
        assert menuResponseBean.getMenuType().equals(Constants.MENU_MODULE);
        assert menuResponseBean.getOptions().get(0).equals("Basic Form Tests");
        String sessionId = menuResponseBean.getSessionId();

        System.out.println("Session ID: " + sessionId);
        System.out.println("Menu Init: " + menuResponseBean);

        JSONObject menuResponseObject =
                selectMenu("requests/menu/menu_select.json", sessionId, "2");

        System.out.println("Menu Response Bean 1: " + menuResponseObject);

        JSONObject menuResponseObject2 =
                selectMenu("requests/menu/menu_select.json", sessionId);

        System.out.println("Menu Response Bean 2: " + menuResponseObject2);

        assert menuResponseObject2.has("tree");
        assert menuResponseObject2.has("title");

    }

    @Test
    public void testCaseSelect() throws Exception {
        // setup files
        MenuResponseBean menuResponseBean =
                doInstall("requests/install/install.json");
        assert menuResponseBean.getOptions().size() == 12;
        assert menuResponseBean.getMenuType().equals(Constants.MENU_MODULE);
        assert menuResponseBean.getOptions().get(0).equals("Basic Form Tests");
        String sessionId = menuResponseBean.getSessionId();

        System.out.println("Session ID: " + sessionId);
        System.out.println("Case Select Init: " + menuResponseBean);

        JSONObject menuResponseObject =
                selectMenu("requests/menu/menu_select.json", sessionId, "2");

        System.out.println("Case Select Bean 1: " + menuResponseObject);

        JSONObject menuResponseObject2 =
                selectMenu("requests/menu/menu_select.json", sessionId, "1");

        System.out.println("Case Select Bean 2: " + menuResponseObject2);

        JSONObject options = new JSONObject(menuResponseObject2.get("options"));

        System.out.println("Length: " + options.length());

        JSONObject menuResponseObject3 =
                selectMenu("requests/menu/menu_select.json", sessionId, "6");

        System.out.println("Case Select Bean 3: " + menuResponseObject3);

        JSONObject menuResponseObject4 =
                selectMenu("requests/menu/menu_select.json", sessionId, "");

        System.out.println("Case Select Bean 4: " + menuResponseObject4);

    }

    public MenuResponseBean doInstall(String requestPath) throws Exception {
        InstallRequestBean installRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), InstallRequestBean.class);
        ResultActions installResult = mockMvc.perform(get(urlPrepend(Constants.URL_INSTALL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(installRequestBean)));
        String installResultString = installResult.andReturn().getResponse().getContentAsString();
        MenuResponseBean menuResponseBean = mapper.readValue(installResultString,
                MenuResponseBean.class);
        return menuResponseBean;
    }

    public JSONObject selectMenu(String requestPath, String sessionId) throws Exception {
        return selectMenu(requestPath, sessionId, "0");
    }

    public JSONObject selectMenu(String requestPath, String sessionId, String selection) throws Exception {
        MenuSelectBean menuSelectBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), MenuSelectBean.class);
        menuSelectBean.setSessionId(sessionId);
        menuSelectBean.setSelection(selection);
        ResultActions selectResult = mockMvc.perform(get(urlPrepend(Constants.URL_MENU_SELECT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(menuSelectBean)));
        String resultString = selectResult.andReturn().getResponse().getContentAsString();
        JSONObject ret = new JSONObject(resultString);
        return ret;
    }
}