package tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

/**
 * Regression tests for fixed behaviors
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class LoadTest extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("loadtestdomain", "loadtestusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/loadtestrestore.xml";
    }

    @Test
    public void testBadSelection() throws Throwable {
        syncDb();
        System.out.println("Synced.");
    }
}
