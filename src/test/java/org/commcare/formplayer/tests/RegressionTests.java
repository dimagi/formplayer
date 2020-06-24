package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.util.screen.CommCareSessionException;
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
public class RegressionTests extends BaseTestClass{

    @Override
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
        } catch(Exception e) {
            assert e.getCause() instanceof CommCareSessionException;
        }
    }
    
    @Test
    public void testReportModule() throws Exception {
        configureRestoreFactory("modulerelevancydomain", "modulerelevancyusername");
        doInstall("requests/install/modulerelevancy.json");
    }
}
