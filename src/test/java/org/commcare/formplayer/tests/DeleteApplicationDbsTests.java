package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.commcare.formplayer.sqlitedb.ApplicationDB;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.commcare.formplayer.utils.TestContext;

import java.io.File;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class DeleteApplicationDbsTests extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("casetestdomain", "casetestuser");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/casetiles.xml";
    }

    /**
     * Ensures that when an application db exists that a delete db request
     * will successfully delete it.
     * @throws Exception
     */
    @Test
    public void testDeleteApplicationDbsView() throws Exception {
        // Create application db by making an install request
        SQLiteDB db = new ApplicationDB("casetestdomain", "casetestuser", null, "casetestappid");
        CommandListResponseBean menuResponseBean = doInstall("requests/install/install.json");

        File file = new File(db.getDatabaseFileForDebugPurposes());
        assert file.exists();

        NotificationMessage response = deleteApplicationDbs();
        assert !response.isError();

        file = new File(db.getDatabaseFileForDebugPurposes());
        assert !file.exists();
    }

    /**
     * Ensures that if no application db exists and a delete db request is
     * made that it successfully returns with status OK.
     * @throws Exception
     */
    @Test
    public void testDeleteApplicationDbsWithNoDbView() throws Exception {
        SQLiteDB db = new ApplicationDB("casetestdomain", "casetestuser", null, "casetestappid");
        assert !new File(db.getDatabaseFileForDebugPurposes()).getParentFile().exists();

        NotificationMessage response = deleteApplicationDbs();
        assert !response.isError();

        assert !new File(db.getDatabaseFileForDebugPurposes()).getParentFile().exists();
    }
}
