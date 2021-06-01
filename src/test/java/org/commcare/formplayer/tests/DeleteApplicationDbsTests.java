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
@ContextConfiguration(classes = TestContext.class)
public class DeleteApplicationDbsTests extends BaseTestClass{

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
