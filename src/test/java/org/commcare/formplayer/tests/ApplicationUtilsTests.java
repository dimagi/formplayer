package org.commcare.formplayer.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.commcare.formplayer.sqlitedb.ApplicationDB;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.commcare.formplayer.utils.TestContext;

import java.io.File;


/**
 * Tests ApplicationUtils
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class ApplicationUtilsTests {

    @Test
    public void testDeleteApplicationDbs() throws Exception {
        SQLiteDB db = new ApplicationDB("dummy-domain", "dummy-username", null, "dummy-app-id");
        db.createDatabaseFolder();

        assert new File(db.getDatabaseFileForDebugPurposes()).getParentFile().exists();

        db.deleteDatabaseFolder();

        assert !new File(db.getDatabaseFileForDebugPurposes()).getParentFile().exists();
    }
}
