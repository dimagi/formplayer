package tests;

import auth.HqAuth;
import beans.NewFormSessionResponse;
import beans.menus.CommandListResponseBean;
import beans.menus.DisplayElement;
import beans.menus.EntityDetailResponse;
import beans.menus.EntityListResponse;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;
import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by willpride on 4/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class DoubleManagementTest  extends BaseTestClass{

    @Override
    public void setUp() throws IOException {
        super.setUp();
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

        JSONObject menuResponseObject =
                sessionNavigate(new String[] {"2"}, "doublemgmt");

        EntityListResponse entityListResponse =
                mapper.readValue(menuResponseObject.toString(), EntityListResponse.class);

        assert entityListResponse.getEntities().length == 2;
        assert entityListResponse.getTitle().equals("Parent (2)");
        assert entityListResponse.getAction() != null;
        assert entityListResponse.getAction().getText().equals("New Parent");
        JSONObject actionResponseObject = sessionNavigate(new String[] {"2", "action 0"}, "doublemgmt");

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

        JSONObject menuResponseObject =
                sessionNavigate(new String[] {"2"}, "doublemgmt");

        EntityListResponse entityListResponse =
                mapper.readValue(menuResponseObject.toString(), EntityListResponse.class);

        assert entityListResponse.getEntities().length == 2;
        assert entityListResponse.getTitle().equals("Parent (2)");
        assert entityListResponse.getAction() != null;
        assert entityListResponse.getAction().getText().equals("New Parent");

        EntityDetailResponse newFormSessionResponse = entityListResponse.getEntities()[0].getDetail();

        assert newFormSessionResponse.getTitle().equals("Details");
        assert newFormSessionResponse.getDetails().length == 1;
    }

    @Test
    public void testNavigator() throws Exception {
        JSONObject sessionNavigateResponse =
                sessionNavigate("requests/navigators/navigator_0.json");
        NewFormSessionResponse newFormSessionResponse =
                mapper.readValue(sessionNavigateResponse.toString(), NewFormSessionResponse.class);
        assert newFormSessionResponse.getTitle().equals("Update Parent");
        assert newFormSessionResponse.getTree().length == 2;
    }

    @Test
    public void testAllPermutations() throws Exception {
        JSONObject parentResponseObject = sessionNavigate(new String[] {"0", "0"}, "doublemgmt");
        NewFormSessionResponse newFormSessionResponse =
                mapper.readValue(parentResponseObject.toString(), NewFormSessionResponse.class);
        assert newFormSessionResponse.getTitle().equals("Register Parent");
        assert newFormSessionResponse.getTree().length == 2;

        JSONObject childResponseObject = sessionNavigate(new String[] {"1", "0"}, "doublemgmt");
        newFormSessionResponse =
                mapper.readValue(childResponseObject.toString(), NewFormSessionResponse.class);
        assert newFormSessionResponse.getTitle().equals("Child Registration");
        assert newFormSessionResponse.getTree().length == 2;

        JSONObject parentResponseObject2 = sessionNavigate(new String[] {"2"}, "doublemgmt");
        EntityListResponse entityListResponse =
                mapper.readValue(parentResponseObject2.toString(), EntityListResponse.class);
        assert entityListResponse.getTitle().equals("Parent (2)");
        assert entityListResponse.getEntities().length == 2;
        assert entityListResponse.getAction() != null;
        DisplayElement action = entityListResponse.getAction();
        assert action.getText().equals("New Parent");

        parentResponseObject2 = sessionNavigate(new String[] {"2", "0"}, "doublemgmt");
        CommandListResponseBean commandListResponse =
                mapper.readValue(parentResponseObject2.toString(), CommandListResponseBean.class);
        assert commandListResponse.getTitle().equals("Parent Child");
        assert commandListResponse.getCommands().length == 2;
        assert commandListResponse.getCommands()[0].getDisplayText().equals("Update Parent");
        assert commandListResponse.getCommands()[1].getDisplayText().equals("Parent Register Child");

        childResponseObject = sessionNavigate(new String[] {"2", "0", "0"}, "doublemgmt");
        newFormSessionResponse =
                mapper.readValue(childResponseObject.toString(), NewFormSessionResponse.class);
        assert newFormSessionResponse.getTitle().equals("Update Parent");
        assert newFormSessionResponse.getTree().length == 2;

    }

    @Test
    public void testMenuMedia() throws Exception {
        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/case_media.json");
        assert menuResponseBean.getCommands().length == 2;
        assert menuResponseBean.getCommands()[0].getDisplayText().equals("Registration");
        assert menuResponseBean.getCommands()[0].getAudioUri().equals("jr://file/commcare/audio/module0_form0_en.mp3");
        assert menuResponseBean.getCommands()[0].getImageUri().equals("jr://file/commcare/image/module0_form0_en.png");
        assert menuResponseBean.getCommands()[1].getDisplayText().equals("Follow Up");
        assert menuResponseBean.getCommands()[1].getAudioUri().equals("jr://file/commcare/audio/module1_en.mp3");
        assert menuResponseBean.getCommands()[1].getImageUri().equals("jr://file/commcare/image/module1_en.png");
    }

    @Test
    public void testEndOfFormNavigation() throws Exception {
        JSONObject parentResponseObject = sessionNavigate(new String[] {"0"}, "endform");
        CommandListResponseBean response0 =
                mapper.readValue(parentResponseObject.toString(), CommandListResponseBean.class);
        System.out.println("response 0: " + response0);
        assert response0.getCommands().length == 2;
        assert response0.getCommands()[0].getDisplayText().equals("Link to Module 1");
        assert response0.getCommands()[1].getDisplayText().equals("Link to Module Menu");

        JSONObject formResponseObject = sessionNavigate(new String[] {"0", "0"}, "endform");
        NewFormSessionResponse newFormSessionResponse =
                mapper.readValue(formResponseObject.toString(), NewFormSessionResponse.class);
        assert newFormSessionResponse.getTitle().equals("Link to Module 1");
        assert newFormSessionResponse.getTree().length == 4;
    }

}
