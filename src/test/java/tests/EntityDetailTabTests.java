package tests;

import beans.menus.EntityDetailListResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class EntityDetailTabTests extends BaseTestClass {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("rmnchdomain", "rmnchusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/rmnch.xml";
    }

    // Regression test confirming that variables are passed to detail tab sub contexts
    @Test
    public void testDetailTabChildVariable() throws Exception {
        // just need to ensure this doesn't error
        getDetails(
                new String[]{
                        "5",
                        "2",
                        "fc3f21b48ff3d56a164becc4ab3f6cb6",
                        "cc478ff8-3ca1-4641-84e7-96b1123371f5"
                },
                "rmnch",
                EntityDetailListResponse.class);
    }
}
