package org.commcare.formplayer.tests;

import org.apache.commons.io.FileUtils;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.sqlitedb.UserDB;
import org.commcare.formplayer.utils.GenerateSnapshotDatabases;
import org.commcare.formplayer.utils.TestContext;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.Iterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import java.io.File;
import java.io.IOException;

/**
 * Tests the compatibility of the current code runtime against the currently captured database
 * snapshots.
 *
 * If these tests fail, the snapshots need to be rebuild and databases will need to be cold started
 * in any existing environments after updating.
 *
 * @author ctsims
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class SnapshotTests extends BaseTestClass {

    @BeforeEach
    private void copySnapshotResources() {
        try {
            File destDirectory = new File(getDatabaseFolderRoot());

            FileUtils.copyDirectory(new File(GenerateSnapshotDatabases.snapshotDbDirectory)
                    , destDirectory);
        } catch (IOException exception) {
            exception.printStackTrace();
            Assertions.fail("Error moving snapshot data to test directory");
        }
    }

    @Test
    public void testUserSandbox() throws Exception {
        UserDB database = getUserDbConnector("snapshot", "snapshot_test", null);
        if (!database.databaseFileExists()) {
            Assertions.fail("Snapshot UserDB Missing for tests you may need to rebuild the snapshot " +
                    "with the CreateSnapshotDbs Gradle Task ");
        }
        //Try to enumerate records of each type
        UserSqlSandbox sandbox = new UserSqlSandbox(database);
        enumerate("Case", sandbox.getCaseStorage());
        enumerate("Ledger", sandbox.getLedgerStorage());
        enumerate("User", sandbox.getUserStorage());
        enumerate("App Fixtures", sandbox.getAppFixtureStorage());
        enumerate("User Fixtures", sandbox.getUserFixtureStorage());
    }

    @Test
    public void testAppSandbox() throws Exception {
        try {
            //Note - Currently this is actually expected to blow away the DB a surprisingly large
            //percentage of the time, but should be expanded in the future to be more robust
            //when app installs are less fragile
            doInstall("sandbox_reference/snapshot.json");
        } catch (Exception e) {
            throw generateSandboxException("App installation", e);
        }
    }

    private void enumerate(String descriptor, IStorageUtilityIndexed<?> storage) throws Exception {
        try {
            System.out.println(String.format("%s: %d", descriptor, storage.getNumRecords()));
            for (Iterator i = storage.iterate(); i.hasMore(); ) {
                Object p = i.nextRecord();
                if (p instanceof IMetaData) {
                    for (String metaField : ((IMetaData)p).getMetaDataFields()) {
                        Object value = ((IMetaData)p).getMetaData(metaField);
                        storage.getIDsForValue(metaField, value == null ? "" : value.toString());
                    }
                }
            }
        } catch (Exception e) {
            throw generateSandboxException(descriptor, e);
        }
    }

    private Exception generateSandboxException(String descriptor, Exception e) {
        Exception se = new Exception(String.format(
                "Error deserializing current snapshot database for %s, if underlying " +
                        "database schemas have changed, you need to rebuild the snapshot with " +
                        "the CreateSnapshotDbs Gradle Task", descriptor
        ));
        se.initCause(e);
        return se;
    }
}
