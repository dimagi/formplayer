package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.menus.EntityDetailListResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.commcare.formplayer.utils.TestContext;

/**
 * Regression tests for fixed behaviors
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class DetailNodesetTest extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("detailnodesetdomain", "detailnodesetusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/testpcp2user1.xml";
    }

    @Test
    public void testBadModuleFilter() throws Exception {
        EntityDetailListResponse response =
                getDetails(new String[]{"0", "9fda59ce-3305-49c7-893f-8fa650883ec2",
                "2", "bf6432f5-090e-4a74-b5b9-75357d8bea97"},
                "enikshay-private", EntityDetailListResponse.class);
        assert response.getEntityDetailList().length == 2;
    }
}
