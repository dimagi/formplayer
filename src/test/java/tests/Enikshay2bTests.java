package tests;

import beans.menus.EntityDetailListResponse;
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
public class Enikshay2bTests extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("enikshay2bdomain", "enikshay2busername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/enikshay-2b.xml";
    }

    @Test
    public void testInlineCaseTile() throws Exception {

        EntityDetailListResponse detailsInline =
                getDetailsInline(
                        new String[]{"0", "905a9f53-2854-4906-acbd-2923f785c3ee"},
                        "try",
                        EntityDetailListResponse.class);
        assert detailsInline.getEntityDetailList().length == 8;

    }
}
