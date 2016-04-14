package tests;

import beans.menus.CommandListResponseBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.javarosa.core.services.storage.StorageManager;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

import java.io.IOException;
import java.util.Arrays;

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
        assert menuResponseBean.getTitle().equals("Basic Tests");
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