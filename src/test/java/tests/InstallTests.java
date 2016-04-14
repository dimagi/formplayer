package tests;

import application.FormController;
import application.MenuController;
import auth.HqAuth;
import beans.InstallRequestBean;
import beans.MenuResponseBean;
import beans.MenuSelectBean;
import beans.menus.Command;
import beans.menus.CommandListResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import install.FormplayerConfigEngine;
import objects.SerializableMenuSession;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.javarosa.core.services.storage.StorageManager;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import services.InstallService;
import services.RestoreService;
import services.XFormService;
import util.Constants;
import utils.FileUtils;
import utils.TestContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Created by willpride on 1/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class InstallTests extends BaseMenuTestClass {

    Log log = LogFactory.getLog(InstallTests.class);

    @Override
    public void setUp() throws IOException {
        super.setUp();
    }


    @Test
    public void testDoubleForm() throws Exception {
        // setup files
        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/double_mgmt_install.json");
        System.out.println("Commands: " + Arrays.toString(menuResponseBean.getCommands()));
        assert menuResponseBean.getCommands().length == 3;
        System.out.println("Title 1: " + menuResponseBean.getTitle());
        //assert menuResponseBean.getTitle().equals("Home");
        System.out.println("Title 2: " + menuResponseBean.getCommands()[0].getDisplayText());
        assert menuResponseBean.getCommands()[0].getDisplayText().equals("Parent");
        String sessionId = menuResponseBean.getSessionId();

        JSONObject menuResponseObject =
                selectMenu("requests/menu/menu_select.json", sessionId);
        JSONObject menuResponseObject2 =
                selectMenu("requests/menu/menu_select.json", sessionId);

        assert menuResponseObject2.has("tree");
        assert menuResponseObject2.has("title");
    }

    @Test
    public void testNewForm() throws Exception {
        // setup files
        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/install.json");
        assert menuResponseBean.getCommands().length == 12;
        System.out.println("Title 1: " + menuResponseBean.getTitle());
        assert menuResponseBean.getTitle().equals("Remove point");
        System.out.println("Title 2: " + menuResponseBean.getCommands()[0].getDisplayText());
        assert menuResponseBean.getCommands()[0].getDisplayText().equals("Basic Form Tests");
        String sessionId = menuResponseBean.getSessionId();

        JSONObject menuResponseObject =
                selectMenu("requests/menu/menu_select.json", sessionId);
        JSONObject menuResponseObject2 =
               selectMenu("requests/menu/menu_select.json", sessionId);

        assert menuResponseObject2.has("tree");
        assert menuResponseObject2.has("title");

    }


    @Test
    public void testCaseCreate() throws Exception {
        // setup files
        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/install.json");
        String sessionId = menuResponseBean.getSessionId();

        JSONObject menuResponseObject =
                selectMenu("requests/menu/menu_select.json", sessionId, "2");

        JSONObject menuResponseObject2 =
                selectMenu("requests/menu/menu_select.json", sessionId);

        assert menuResponseObject2.has("tree");
        assert menuResponseObject2.has("title");
        SqlSandboxUtils.deleteDatabaseFolder("dbs");
        StorageManager.forceClear();
    }


    @Test
    public void testCaseSelect() throws Exception {
        SqlSandboxUtils.deleteDatabaseFolder("dbs");
        // setup files
        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/install.json");
        String sessionId = menuResponseBean.getSessionId();

        JSONObject menuResponseObject =
                selectMenu("requests/menu/menu_select.json", sessionId, "2");

        JSONObject menuResponseObject2 =
                selectMenu("requests/menu/menu_select.json", sessionId, "1");

        JSONObject menuResponseObject3 =
                selectMenu("requests/menu/menu_select.json", sessionId, "6");
        JSONObject menuResponseObject4 =
                selectMenu("requests/menu/menu_select.json", sessionId, "");
        SqlSandboxUtils.deleteDatabaseFolder("dbs");
        StorageManager.forceClear();
    }

    @After
    public void tearDown(){
        //SqlSandboxUtils.deleteDatabaseFolder("dbs");
    }
}