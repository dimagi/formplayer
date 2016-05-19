package tests;

import application.MenuController;
import auth.HqAuth;
import beans.InstallRequestBean;
import beans.SessionNavigationBean;
import beans.menus.CommandListResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import install.FormplayerConfigEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import repo.SessionRepo;
import services.InstallService;
import services.RestoreService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Created by willpride on 4/13/16.
 */
public class BaseMenuTestClass {
    private MockMvc mockMvc;

    @Autowired
    private SessionRepo sessionRepoMock;

    @Autowired
    private XFormService xFormServiceMock;

    @Autowired
    RestoreService restoreServiceMock;

    @Autowired
    private InstallService installService;

    @InjectMocks
    MenuController menuController;

    ObjectMapper mapper;

    private String urlPrepend(String string){
        return "/" + string;
    }

    private Log log = LogFactory.getLog(BaseMenuTestClass.class);

    @Before
    public void setUp() throws IOException {
        Mockito.reset(sessionRepoMock);
        Mockito.reset(xFormServiceMock);
        Mockito.reset(restoreServiceMock);
        Mockito.reset(installService);
        MockitoAnnotations.initMocks(this);
        mapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(menuController).build();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"));
        setupInstallServiceMock();
    }

    private String resolveAppId(String ref){
        log.info("Test resolving hack ref: " + ref);
        String appId = ref.substring(ref.indexOf("app_id=") + "app_id=".length(),
                ref.indexOf("#hack"));
        log.info("Got appId: " + appId);
        switch (appId) {
            case "doublemgmtappid":
                ref = "apps/basic2/profile.ccpr";
                break;
            case "navigatorappid":
                ref = "apps/basic2/profile.ccpr";
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
            default:
                throw new RuntimeException("Couldn't resolve appId for ref: " + ref);
        }
        log.info("Resolved ref: " + ref);
        return ref;
    }

    private void setupInstallServiceMock() throws IOException {
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
            log.error("Couldn't find resource at path " + resourcePath);
            npe.printStackTrace();
            throw npe;
        }
    }

    JSONObject sessionNavigate(String requestPath) throws Exception {
        SessionNavigationBean sessionNavigationBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), SessionNavigationBean.class);
        ResultActions selectResult = mockMvc.perform(
                post(urlPrepend(Constants.URL_MENU_NAVIGATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("sessionid", "derp"))
                        .content(mapper.writeValueAsString(sessionNavigationBean)));
        String resultString = selectResult.andReturn().getResponse().getContentAsString();
        return new JSONObject(resultString);
    }

    JSONObject sessionNavigate(String[] selections, String testName) throws Exception {
        SessionNavigationBean sessionNavigationBean = new SessionNavigationBean();
        sessionNavigationBean.setDomain(testName + "domain");
        sessionNavigationBean.setAppId(testName + "appid");
        sessionNavigationBean.setUsername(testName + "username");
        sessionNavigationBean.setSelections(selections);
        ResultActions selectResult = mockMvc.perform(
                post(urlPrepend(Constants.URL_MENU_NAVIGATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new Cookie("sessionid", "derp"))
                        .content(mapper.writeValueAsString(sessionNavigationBean)));
        String resultString = selectResult.andReturn().getResponse().getContentAsString();
        return new JSONObject(resultString);
    }

    CommandListResponseBean doInstall(String requestPath) throws Exception {
        InstallRequestBean installRequestBean = mapper.readValue
                (FileUtils.getFile(this.getClass(), requestPath), InstallRequestBean.class);
        ResultActions installResult = mockMvc.perform(
                post(urlPrepend(Constants.URL_INSTALL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(installRequestBean)));
        String installResultString = installResult.andReturn().getResponse().getContentAsString();
        CommandListResponseBean menuResponseBean = mapper.readValue(installResultString,
                CommandListResponseBean.class);
        return menuResponseBean;
    }
}
