package tests;

import beans.menus.EntityDetailListResponse;
import beans.menus.EntityDetailResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

/**
 * Tests specific to Enikshay
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class EnikshayTests extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("enikshaydomain", "enikshayusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/enikshay.xml";
    }

    @Test
    public void testPersistentCaseTile() throws Exception {
        EntityDetailListResponse details =
                getDetails(
                        new String[]{"0", "7e2e508b-42c3-4fe5-a216-c8d5473ab43b", "0"},
                        "enikshay",
                        EntityDetailListResponse.class);
    }
}
