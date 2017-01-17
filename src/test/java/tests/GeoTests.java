package tests;

import beans.menus.EntityListResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

/**
 * Tests for geo functionality
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class GeoTests extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("heredoman", "hereusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/geo.xml";
    }

    // validate that we don't crash on here() function
    @Test
    public void testHereOverride() throws Exception {
        sessionNavigate(new String[]{"5", "1"}, "basic", EntityListResponse.class);
    }
}
