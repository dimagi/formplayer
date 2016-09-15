package tests;

import auth.HqAuth;
import beans.NewFormResponse;
import beans.SubmitResponseBean;
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

        NewFormResponse newFormResponse =
                mapper.readValue(actionResponseObject.toString(), NewFormResponse.class);

        assert newFormResponse.getTitle().equals("Register Parent");
        assert newFormResponse.getTree().length == 2;

        // ok, test end of form nav
        SubmitResponseBean submitResponseBean = submitForm("requests/submit/submit_double_mgmt.json",
                newFormResponse.getSessionId());
        assert submitResponseBean.getNextScreen() != null;
        CommandListResponseBean commandListResponseBean = mapper.readValue(mapper.writeValueAsString(submitResponseBean.getNextScreen()),
                CommandListResponseBean.class);
        assert commandListResponseBean.getCommands().length == 2;
        assert commandListResponseBean.getCommands()[0].getDisplayText().equals("Update Parent");
        JSONObject endOfFormNavResponse = sessionNavigateWithId(new String[] {"0"}, "derp");
        NewFormResponse followupFormResponse =
                mapper.readValue(endOfFormNavResponse.toString(), NewFormResponse.class);
        assert followupFormResponse.getTree().length == 2;
        assert followupFormResponse.getTree()[0].getAnswer().equals("David Ortiz");
        assert followupFormResponse.getTree()[1].getAnswer().equals(40);
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

        EntityDetailResponse newFormSessionResponse = entityListResponse.getEntities()[0].getDetails()[0];

        assert newFormSessionResponse.getTitle().equals("Cases");
        assert newFormSessionResponse.getDetails().length == 1;
    }

    @Test
    public void testNavigator() throws Exception {
        JSONObject sessionNavigateResponse =
                sessionNavigate("requests/navigators/navigator_0.json");
        NewFormResponse newFormResponse =
                mapper.readValue(sessionNavigateResponse.toString(), NewFormResponse.class);
        assert newFormResponse.getTitle().equals("Update Parent");
        assert newFormResponse.getTree().length == 2;
    }

    @Test
    public void testAllPermutations() throws Exception {
        JSONObject parentResponseObject = sessionNavigate(new String[] {"0", "0"}, "doublemgmt");
        NewFormResponse newFormResponse =
                mapper.readValue(parentResponseObject.toString(), NewFormResponse.class);
        assert newFormResponse.getTitle().equals("Register Parent");
        assert newFormResponse.getTree().length == 2;

        JSONObject childResponseObject = sessionNavigate(new String[] {"1", "0"}, "doublemgmt");
        newFormResponse =
                mapper.readValue(childResponseObject.toString(), NewFormResponse.class);
        assert newFormResponse.getTitle().equals("Child Registration");
        assert newFormResponse.getTree().length == 2;

        JSONObject parentResponseObject2 = sessionNavigate(new String[] {"2"}, "doublemgmt");
        EntityListResponse entityListResponse =
                mapper.readValue(parentResponseObject2.toString(), EntityListResponse.class);
        assert entityListResponse.getTitle().equals("Parent (2)");
        assert entityListResponse.getEntities().length == 3;
        assert entityListResponse.getAction() != null;
        DisplayElement action = entityListResponse.getAction();
        assert action.getText().equals("New Parent");

        parentResponseObject2 = sessionNavigate(new String[] {"2", "4d1831ab-abfe-4086-bce7-16d325d9ca3a"}, "doublemgmt");
        CommandListResponseBean commandListResponse =
                mapper.readValue(parentResponseObject2.toString(), CommandListResponseBean.class);
        assert commandListResponse.getTitle().equals("Parent (2)");
        assert commandListResponse.getCommands().length == 2;
        assert commandListResponse.getCommands()[0].getDisplayText().equals("Update Parent");
        assert commandListResponse.getCommands()[1].getDisplayText().equals("Parent Register Child");

        childResponseObject = sessionNavigate(new String[] {"2", "4d1831ab-abfe-4086-bce7-16d325d9ca3a", "0"}, "doublemgmt");
        newFormResponse =
                mapper.readValue(childResponseObject.toString(), NewFormResponse.class);
        assert newFormResponse.getTitle().equals("Update Parent");
        assert newFormResponse.getTree().length == 2;

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
        assert response0.getCommands().length == 2;
        assert response0.getCommands()[0].getDisplayText().equals("Link to Module 1");
        assert response0.getCommands()[1].getDisplayText().equals("Link to Module Menu");

        JSONObject formResponseObject = sessionNavigate(new String[] {"0", "0"}, "endform");
        NewFormResponse newFormResponse =
                mapper.readValue(formResponseObject.toString(), NewFormResponse.class);
        assert newFormResponse.getTitle().equals("Link to Module 1");
        assert newFormResponse.getTree().length == 4;
    }

}
