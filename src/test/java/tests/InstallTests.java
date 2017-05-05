package tests;

import beans.NewFormResponse;
import beans.menus.CommandListResponseBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sandbox.SqlSandboxUtils;
import org.javarosa.core.services.storage.StorageManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

/**
 * Created by willpride on 1/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class InstallTests extends BaseTestClass {

    Log log = LogFactory.getLog(InstallTests.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("casetestdomain", "casetestuser");
    }

    @Test
    public void testCaseCreate() throws Exception {
        SqlSandboxUtils.deleteDatabaseFolder("dbs");
        // setup files

        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/install.json");

        NewFormResponse menuResponseObject =
                sessionNavigate(new String[] {"2", "0"}, "case", NewFormResponse.class);
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

        NewFormResponse newFormResponse =
                sessionNavigate(new String[] {"0", "0"}, "case", NewFormResponse.class);

    }

    @Test
    public void testCaseSelect() throws Exception {
        SqlSandboxUtils.deleteDatabaseFolder("dbs");
        NewFormResponse formSessionResponse =
                sessionNavigate(new String[] {"2", "1", "1a8ca44cb5dc4ce9995a71ea8929d4c3"},
                        "case", NewFormResponse.class);
        assert formSessionResponse.getTitle().equals("Update a Case");
        assert formSessionResponse.getTree().length == 7;

        SqlSandboxUtils.deleteDatabaseFolder("dbs");
        StorageManager.instance().forceClear();
    }

}