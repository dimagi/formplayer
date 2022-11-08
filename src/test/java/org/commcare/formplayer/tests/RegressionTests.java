package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.commcare.formplayer.beans.menus.BaseResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

/**
 * Regression tests for fixed behaviors
 */
@WebMvcTest
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
    public void testBadCaseSelection() throws Exception {
        BaseResponseBean response = sessionNavigate(new String[]{"2", "1"}, "doublemgmt",
                EntityListResponse.class);
        assertTrue(response.getNotification().isError(), "Bad case selection should result into an error");
        assertEquals(response.getNotification().getMessage(),
                "Could not select case 1 on this screen.  If this error persists please report a bug to CommCareHQ.");
    }

    @Test
    public void testReportModule() throws Exception {
        configureRestoreFactory("modulerelevancydomain", "modulerelevancyusername");
        doInstall("requests/install/modulerelevancy.json");
    }
}
