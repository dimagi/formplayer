package tests;

import beans.menus.EntityListResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

/**
 * This regression test confirms that the suite-fixture in the scheduler is parsed
 * and stored correctly
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class SchedulerTest extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("schedulerdomain", "schedulerusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/parent_child.xml";
    }

    @Test
    public void testScheduler() throws Exception {
        sessionNavigate(new String[]{"0"}, "scheduler", EntityListResponse.class);
    }
}
