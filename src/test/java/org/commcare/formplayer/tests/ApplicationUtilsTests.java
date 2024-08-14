package org.commcare.formplayer.tests;

import org.commcare.formplayer.sqlitedb.ApplicationDB;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.junit.jupiter.api.Test;

import java.io.File;


public class ApplicationUtilsTests {

    @Test
    public void testDeleteApplicationDbs() throws Exception {
        SQLiteDB db = new ApplicationDB("dummy-domain", "dummy-username", null, "dummy-app-id", null);
        db.createDatabaseFolder();

        assert new File(db.getDatabaseFileForDebugPurposes()).getParentFile().exists();

        db.deleteDatabaseFolder();

        assert !new File(db.getDatabaseFileForDebugPurposes()).getParentFile().exists();

        // Test with app version parent file name
        SQLiteDB db2 = new ApplicationDB("dummy-domain", "dummy-username", null, "dummy-app-id", "7");
        db2.createDatabaseFolder();

        assert new File(db2.getDatabaseFileForDebugPurposes()).getParentFile().exists();

        db2.deleteDatabaseFolder();

        assert !new File(db2.getDatabaseFileForDebugPurposes()).getParentFile().exists();
    }
}
