package tests;

import beans.menus.EntityListResponse;
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
        configureRestoreFactory("enikdomain", "enikusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/test.load.2.xml";
    }

    @Test
    public void loadTest() throws Exception{
        long tStart = System.currentTimeMillis();
        syncDb();
        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;
        double elapsedSeconds = tDelta / 1000.0;
        System.out.println("Elasped " + elapsedSeconds);
    }

    @Test
    public void testEnik() throws Throwable {
        long tStart = System.currentTimeMillis();
        EntityListResponse entityListResponse = sessionNavigate(new String[]{"3"}, "enik2", EntityListResponse.class);
        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;
        double elapsedSeconds = tDelta / 1000.0;
        System.out.println("Elasped " + elapsedSeconds);
    }
}
