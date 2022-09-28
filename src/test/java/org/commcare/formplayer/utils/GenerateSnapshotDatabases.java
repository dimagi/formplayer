package org.commcare.formplayer.utils;

import static org.junit.jupiter.api.Assertions.fail;

import org.commcare.formplayer.application.SQLiteProperties;
import org.commcare.formplayer.application.UtilController;
import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.junit.InitializeStaticsExtension;
import org.commcare.formplayer.junit.Installer;
import org.commcare.formplayer.junit.RestoreFactoryExtension;
import org.commcare.formplayer.junit.request.SyncDbRequest;
import org.commcare.formplayer.sandbox.SqlSandboxUtils;
import org.commcare.formplayer.services.FormplayerLockRegistry;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.MenuSessionFactory;
import org.commcare.formplayer.services.MenuSessionRunnerService;
import org.commcare.formplayer.services.RestoreFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;


/**
 * Not actually a test, this "Test" class uses the same harnesses and mocks as the real tests to
 * produce a snapshot of the database schemas created by the current code.
 *
 * When those implicit schemas are changed, the Snapshot tests will start failing, and will need to
 * be regenerated with the "CreateSnapshotDbs" Gradle task.
 *
 * Reads the input files from the "snapshot_reference" folder to produce a consistent set of dbs for
 * testing. Any new types of models to be tested should be added to that app config and user
 * restore.
 *
 * @author ctsims
 */
@WebMvcTest
@ContextConfiguration(classes={TestContext.class, CacheConfiguration.class})
@Import({UtilController.class})
@TestPropertySource(properties={"sqlite.dataDir=src/test/resources/snapshot/"})
@ExtendWith(InitializeStaticsExtension.class)
public class GenerateSnapshotDatabases {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestoreFactory restoreFactory;

    @Autowired
    private FormplayerStorageFactory storageFactory;

    @Autowired
    private MenuSessionFactory menuSessionFactory;

    @Autowired
    private MenuSessionRunnerService menuSessionRunnerService;

    @MockBean
    private FormplayerLockRegistry lockRegistry;

    @RegisterExtension
    static RestoreFactoryExtension restoreFactoryExt = new RestoreFactoryExtension.builder()
            .withUser("snapshot_test").withDomain("snapshot")
            .withRestorePath("sandbox_reference/user_restore.xml")
            .build();

    @Test
    public void testCreateSnapshot() throws Exception {
        if (inCreateMode()) {
            //clear the landing zone
            SqlSandboxUtils.deleteDatabaseFolder(SQLiteProperties.getDataDir());

            //ensure the landing zone is clear
            if (new File(SQLiteProperties.getDataDir()).exists()) {
                fail("Couldn't remove existing snapshot assets");
            }
            new SyncDbRequest(mockMvc, restoreFactory).request();
            new Installer(
                    restoreFactory,
                    storageFactory,
                    menuSessionFactory,
                    menuSessionRunnerService
            ).doInstall("sandbox_reference/snapshot.json");
        }
    }

    private boolean inCreateMode() {
        return "write".equals(System.getenv("org.commcare.formplayer.test.snapshot.mode"));
    }
}
