package tests;

import auth.HqAuth;
import beans.NewFormSessionResponse;
import beans.menus.CommandListResponseBean;
import beans.menus.EntityDetailResponseBean;
import beans.menus.EntityListResponseBean;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by willpride on 4/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class DoubleManagementTest  extends BaseMenuTestClass{

    @Override
    public void setUp() throws IOException {
        super.setUp();
        SqlSandboxUtils.deleteDatabaseFolder("dbs");
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "restores/parent_child.xml"));
    }

    @Test
    public void testDoubleForm() throws Exception {
        // setup files
        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/double_mgmt_install.json");
        assert menuResponseBean.getCommands().length == 3;
        assert menuResponseBean.getTitle().equals("Parent Child");
        assert menuResponseBean.getCommands()[0].getDisplayText().equals("Parent");
        assert menuResponseBean.getCommands()[1].getDisplayText().equals("Child");
        assert menuResponseBean.getCommands()[2].getDisplayText().equals("Parent (2)");
        String sessionId = menuResponseBean.getSessionId();

        JSONObject menuResponseObject =
                selectMenu("requests/menu/menu_select.json", sessionId, "2");

        EntityListResponseBean entityListResponseBean =
                mapper.readValue(menuResponseObject.toString(), EntityListResponseBean.class);

        assert entityListResponseBean.getEntities().length == 2;
        assert entityListResponseBean.getTitle().equals("Parent (2)");
        assert entityListResponseBean.getAction() != null;
        assert entityListResponseBean.getAction().getText().equals("New Parent");

        JSONObject actionResponseObject =
                selectMenu("requests/menu/menu_select.json", sessionId, "action 0");

        NewFormSessionResponse newFormSessionResponse =
                mapper.readValue(actionResponseObject.toString(), NewFormSessionResponse.class);

        assert newFormSessionResponse.getTitle().equals("Register Parent");
        assert newFormSessionResponse.getTree().length == 2;
    }

    @Test
    public void testDoubleCaseSelect() throws Exception {
        // setup files
        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/double_mgmt_install.json");
        assert menuResponseBean.getCommands().length == 3;
        assert menuResponseBean.getTitle().equals("Parent Child");
        assert menuResponseBean.getCommands()[0].getDisplayText().equals("Parent");
        assert menuResponseBean.getCommands()[1].getDisplayText().equals("Child");
        assert menuResponseBean.getCommands()[2].getDisplayText().equals("Parent (2)");
        String sessionId = menuResponseBean.getSessionId();

        JSONObject menuResponseObject =
                selectMenu("requests/menu/menu_select.json", sessionId, "2");

        EntityListResponseBean entityListResponseBean =
                mapper.readValue(menuResponseObject.toString(), EntityListResponseBean.class);

        assert entityListResponseBean.getEntities().length == 2;
        assert entityListResponseBean.getTitle().equals("Parent (2)");
        assert entityListResponseBean.getAction() != null;
        assert entityListResponseBean.getAction().getText().equals("New Parent");

        JSONObject actionResponseObject =
                selectMenu("requests/menu/menu_select.json", sessionId, "0");

        EntityDetailResponseBean newFormSessionResponse =
                mapper.readValue(actionResponseObject.toString(), EntityDetailResponseBean.class);

        assert newFormSessionResponse.getTitle().equals("Parent (2)");
        assert newFormSessionResponse.getDetails().length == 1;
    }
}
