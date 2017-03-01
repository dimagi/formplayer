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
public class QaRunnerTests extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("qarunnerdomain", "qarunnerusername");
    }
    @Test
    public void runBasicQa() throws Exception{
        runQaPlan("requests/run_qa/basic.json", "qaplans/basic.feature");
    }
}
