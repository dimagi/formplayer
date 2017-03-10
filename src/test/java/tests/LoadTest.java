package tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import parsers.FormplayerCaseXmlParser;
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
        long tStart = System.currentTimeMillis();
        syncDb();
        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;
        double elapsedSeconds = tDelta / 1000.0;
        System.out.println("Synced " + FormplayerCaseXmlParser.caseCount + " in " + elapsedSeconds + " seconds.");
    }
}
