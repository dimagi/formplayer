package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.util.screen.CommCareSessionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.commcare.formplayer.utils.TestContext;

/**
 * Regression tests for fixed behaviors
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class RegressionTests extends BaseTestClass {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("doublemgmtdomain", "doublemgmtusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/parent_child.xml";
    }

    @Test
    public void testBadCaseSelection() {
        try {
            sessionNavigate(new String[]{"2", "1"}, "doublemgmt", NewFormResponse.class);
        } catch (Exception e) {
            assert e.getCause() instanceof CommCareSessionException;
        }
    }

    @Test
    public void testReportModule() throws Exception {
        configureRestoreFactory("modulerelevancydomain", "modulerelevancyusername");
        doInstall("requests/install/modulerelevancy.json");
    }
}
