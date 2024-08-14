package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.sqlitedb.ApplicationDB;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;


@WebMvcTest
public class DeleteApplicationDbsTests extends BaseTestClass {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("casetestdomain", "casetestuser");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/casetiles.xml";
    }

    /**
     * Ensures that when an application db exists that a delete db request will successfully delete
     * it.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteApplicationDbsView() throws Exception {
        // Create application db by making an install request
        SQLiteDB db = new ApplicationDB("casetestdomain", "casetestuser", null, "casetestappid", null);
        doInstall("requests/install/install.json");

        File file = new File(db.getDatabaseFileForDebugPurposes());
        assert file.exists();

        NotificationMessage response = deleteApplicationDbs("requests/delete_db/delete_db.json");
        assert !response.isError();

        file = new File(db.getDatabaseFileForDebugPurposes());
        assert !file.exists();

        // Test with app version in install
        SQLiteDB db2 = new ApplicationDB("casetestdomain", "casetestuser", null, "casetestappid", "7");
        doInstall("requests/install/install_with_app_version.json");

        file = new File(db2.getDatabaseFileForDebugPurposes());
        assert file.exists();

        response = deleteApplicationDbs("requests/delete_db/delete_db_with_app_version.json");
        assert !response.isError();

        file = new File(db2.getDatabaseFileForDebugPurposes());
        assert !file.exists();
    }

    /**
     * Ensures that if no application db exists and a delete db request is made that it successfully
     * returns with status OK.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteApplicationDbsWithNoDbView() throws Exception {
        SQLiteDB db = new ApplicationDB("casetestdomain", "casetestuser", null, "casetestappid", null);
        assert !new File(db.getDatabaseFileForDebugPurposes()).getParentFile().exists();

        NotificationMessage response = deleteApplicationDbs("requests/delete_db/delete_db.json");
        assert !response.isError();

        assert !new File(db.getDatabaseFileForDebugPurposes()).getParentFile().exists();
    }
}
