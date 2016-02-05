package tests;

import application.Application;
import application.InstallController;
import application.SessionController;
import auth.HqAuth;
import beans.InstallResponseBean;
import beans.MenuResponseBean;
import beans.MenuSelectBean;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Created by willpride on 1/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class InstallTests extends BaseTestClass{

    @InjectMocks
    InstallController installController;

    @Before
    @Override
    public void setUp() throws IOException {
        super.setUp();
        mockMvc = MockMvcBuilders.standaloneSetup(installController).build();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"));
    }

    @Test
    public void testNewForm() throws Exception {
        // setup files
        MenuResponseBean menuResponseBean =
                doInstall("requests/install/install.json");
        assert menuResponseBean.getOptions().size() == 12;
        assert menuResponseBean.getMenuType().equals(Constants.MENU_MODULE);
        assert menuResponseBean.getOptions().get(0).equals("Basic Form Tests");

        //MenuResponseBean menuResponseBean1 =
        //        selectMenu("requests/menu/menu_select.json");

    }
}