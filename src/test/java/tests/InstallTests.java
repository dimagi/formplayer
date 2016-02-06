package tests;

import application.Application;
import application.InstallController;
import application.SessionController;
import auth.HqAuth;
import beans.InstallRequestBean;
import beans.InstallResponseBean;
import beans.MenuResponseBean;
import beans.MenuSelectBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableMenuSession;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
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
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import repo.MenuRepo;
import repo.SessionRepo;
import services.RestoreService;
import services.XFormService;
import util.Constants;
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
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

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
    InstallController installController;

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
        mockMvc = MockMvcBuilders.standaloneSetup(installController).build();
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

        MenuResponseBean menuResponseBean1 =
                selectMenu("requests/menu/menu_select.json");

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

    public MenuResponseBean selectMenu(String requestPath) throws Exception {
        MenuSelectBean menuSelectBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), MenuSelectBean.class);
        ResultActions selectResult = mockMvc.perform(get(urlPrepend(Constants.URL_MENU_SELECT))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(menuSelectBean)));
        String resultString = selectResult.andReturn().getResponse().getContentAsString();
        MenuResponseBean menuResponseBean = mapper.readValue(resultString,
                MenuResponseBean.class);
        return menuResponseBean;
    }
}