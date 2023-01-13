package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.menus.EntityDetailListResponse;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Regression tests for fixed behaviors
 */
@WebMvcTest
public class DetailNodesetTest extends BaseTestClass {

    @Override
    @BeforeEach
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
