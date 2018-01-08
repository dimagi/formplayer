package tests;

import beans.NewFormResponse;
import beans.SubmitResponseBean;
import beans.menus.CommandListResponseBean;
import beans.menus.EntityListResponse;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

import java.util.LinkedHashMap;

/**
 * Test that demonstrates the failure to parse a Demo User restore into storage
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class DemoUserTest extends BaseTestClass {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("demo_user", "demo_user");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/basic.xml";
    }

    @Test
    public void testPersistentCaseTile() throws Exception {
        NewFormResponse newFormResponse =
                sessionNavigate(
                        new String[]{"0"},
                        "demo_user",
                        NewFormResponse.class);
    }
}
