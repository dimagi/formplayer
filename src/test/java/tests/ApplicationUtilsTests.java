package tests;

import beans.menus.CommandListResponseBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import util.ApplicationUtils;
import utils.TestContext;

import java.io.File;


/**
 * Tests ApplicationUtils
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class ApplicationUtilsTests extends BaseTestClass {

    @Test
    public void testDeleteApplicationDbs() throws Exception {
        String appId = "my-app-id";
        File file = new File("dbs/" + appId);
        file.mkdirs();

        assert file.exists();

        Boolean success = ApplicationUtils.deleteApplicationDbs(appId);

        file = new File("dbs/" + appId);
        assert !file.exists();
        assert success;
    }
}
