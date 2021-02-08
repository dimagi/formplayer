package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.commcare.formplayer.utils.TestContext;

/**
 * This regression test confirms that the suite-fixture in the scheduler is parsed
 * and stored correctly
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class SchedulerTest extends BaseTestClass{

    @Override
    @BeforeEach
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
