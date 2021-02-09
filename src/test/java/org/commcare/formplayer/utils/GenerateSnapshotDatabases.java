package org.commcare.formplayer.utils;

import org.commcare.formplayer.application.SQLiteProperties;
import org.commcare.formplayer.sandbox.SqlSandboxUtils;
import org.commcare.formplayer.tests.BaseTestClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * Not actually a test, this "Test" class uses the same harnesses and mocks as the real tests
 * to produce a snapshot of the database schemas created by the current code.
 *
 * When those implicit schemas are changed, the Snapshot tests will start failing, and will need
 * to be regenerated with the "CreateSnapshotDbs" Gradle task.
 *
 * Reads the input files from the "snapshot_reference" folder to produce a consistent set of
 * dbs for testing. Any new types of models to be tested should be added to that app config
 * and user restore.
 *
 * @author ctsims
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class GenerateSnapshotDatabases extends BaseTestClass {

    //This is the destination directory for the snapshot.
    public static String snapshotDbDirectory = "src/test/resources/snapshot/";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("snapshot", "snapshot_test");
    }

    protected String getDatabaseFolderRoot() {
        return snapshotDbDirectory;
    }

    @Override
    protected boolean removeDatabaseFoldersAfterTests() {
        return false;
    }

    @Override
    protected String getMockRestoreFileName() {
        return "sandbox_reference/user_restore.xml";
    }

    @Test
    public void testCreateSnapshot() throws Exception {
        if(inCreateMode()) {
            //clear the landing zone
            SqlSandboxUtils.deleteDatabaseFolder(SQLiteProperties.getDataDir());

            //ensure the landing zone is clear
            if (new File(SQLiteProperties.getDataDir()).exists()) {
                fail("Couldn't remove existing snapshot assets");
            }
            syncDb();
            doInstall("sandbox_reference/snapshot.json");
        }
    }

    private boolean inCreateMode() {
        return "write".equals(System.getenv("org.commcare.formplayer.test.snapshot.mode"));
    }
}