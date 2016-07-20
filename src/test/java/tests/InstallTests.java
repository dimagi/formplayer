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

/**
 * Created by willpride on 1/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class InstallTests extends BaseMenuTestClass {

    Log log = LogFactory.getLog(InstallTests.class);

    @Test
    public void testCaseCreate() throws Exception {
        SqlSandboxUtils.deleteDatabaseFolder("dbs");
        // setup files

        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/install.json");

        JSONObject menuResponseObject = sessionNavigate(new String[] {"2", "0"}, "create");

        assert menuResponseObject.has("tree");
        assert menuResponseObject.has("title");
        SqlSandboxUtils.deleteDatabaseFolder("dbs");
    }


    @Test
    public void testNewForm() throws Exception {
        // setup files
        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/install.json");
        assert menuResponseBean.getCommands().length == 12;
        assert menuResponseBean.getTitle().equals("Basic Tests");
        assert menuResponseBean.getCommands()[0].getDisplayText().equals("Basic Form Tests");

        JSONObject menuResponseObject = sessionNavigate(new String[] {"0", "0"}, "case");

        assert menuResponseObject.has("tree");
        assert menuResponseObject.has("title");

    }

    @Test
    public void testCaseSelect() throws Exception {
        SqlSandboxUtils.deleteDatabaseFolder("dbs");
        // setup files
        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/case_create_install.json");

        JSONObject menuResponseObject = sessionNavigate(new String[] {"2", "1", "6"}, "case");

        SqlSandboxUtils.deleteDatabaseFolder("dbs");
        StorageManager.forceClear();
    }

}