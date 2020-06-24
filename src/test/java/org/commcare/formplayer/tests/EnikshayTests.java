package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.menus.EntityDetailListResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.commcare.formplayer.utils.TestContext;

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

    @Test
    public void testBreadcrumbs() throws Exception {
        NewFormResponse newFormResponse =
                sessionNavigate(
                        new String[]{"0", "7e2e508b-42c3-4fe5-a216-c8d5473ab43b", "0"},
                        "enikshay",
                        NewFormResponse.class);
        assert newFormResponse.getBreadcrumbs().length == 4;
        // Assert we see the case name
        assert newFormResponse.getBreadcrumbs()[2].equals("252923 Test");
    }
}
