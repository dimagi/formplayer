package tests;

import beans.menus.CommandListResponseBean;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

import java.util.Arrays;

/**
 * Created by willpride on 4/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class DoubleManagementTest  extends BaseMenuTestClass{

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
}
