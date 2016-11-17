package tests;

import beans.menus.CommandListResponseBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class UpdateTests extends BaseTestClass {

    Log log = LogFactory.getLog(UpdateTests.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("updatetestdomain", "updatetestuser");
    }

    @Test
    public void testUpdate() throws Exception {
        CommandListResponseBean menuResponseBean =
                doInstall("requests/install/install_basic.json");
        CommandListResponseBean updateResponseBean =
                doUpdate("requests/install/install_basic.json");
    }

}