package org.commcare.formplayer.tests;

import org.commcare.formplayer.sqlitedb.ApplicationDB;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.junit.jupiter.api.Test;

import java.io.File;


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
