package tests;

import beans.NewFormResponse;
import beans.SubmitResponseBean;
import beans.menus.CommandListResponseBean;
import beans.menus.DisplayElement;
import beans.menus.EntityDetailResponse;
import beans.menus.EntityListResponse;
import org.commcare.util.screen.CommCareSessionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.FileUtils;
import utils.TestContext;

import static org.mockito.Mockito.when;

/**
 * Regression tests for fixed behaviors
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class RegressionTests extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(restoreFactoryMock.getRestoreXml())
                .thenReturn(FileUtils.getFile(this.getClass(), "restores/parent_child.xml"));
        configureRestoreFactory("doublemgmtdomain", "doublemgmtusername");
    }

    @Test(expected=CommCareSessionException.class)
    public void testBadCaseSelection() throws Throwable {
        try {
            sessionNavigate(new String[]{"2", "1"}, "doublemgmt", NewFormResponse.class);
        } catch(Exception e) {
            throw e.getCause();
        }
    }
}
