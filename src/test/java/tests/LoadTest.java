package tests;

/**
 * Regression tests for fixed behaviors
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = TestContext.class)
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

    public void testBadSelection() throws Throwable {
        long tStart = System.currentTimeMillis();
        syncDb();
        long tEnd = System.currentTimeMillis();
        long tDelta = tEnd - tStart;
        double elapsedSeconds = tDelta / 1000.0;
        System.out.println("Synced in " + elapsedSeconds + " seconds.");
    }
}
