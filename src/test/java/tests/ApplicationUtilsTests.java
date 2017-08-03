package tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import sqlitedb.ApplicationDB;
import util.ApplicationUtils;
import utils.TestContext;

import java.io.File;


/**
 * Tests ApplicationUtils
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class ApplicationUtilsTests {

    @Test
    public void testDeleteApplicationDbs() throws Exception {
        String dbPath = ApplicationUtils.getApplicationDBPath("dummy-domain", "dummy-username", null, "dummy-app-id");
        File file = new File(dbPath);
        file.mkdirs();

        assert file.exists();

        new ApplicationDB("dummy-domain", "dummy-username", null, "dummy-app-id").deleteDatabaseFolder();

        file = new File(dbPath);
        assert !file.exists();
    }
}
