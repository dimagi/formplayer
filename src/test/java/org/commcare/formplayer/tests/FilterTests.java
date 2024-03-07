package org.commcare.formplayer.tests;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.commcare.cases.model.Case;
import org.commcare.formplayer.application.SQLiteProperties;
import org.commcare.formplayer.application.UtilController;
import org.commcare.formplayer.beans.SyncDbResponseBean;
import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.junit.InitializeStaticsExtension;
import org.commcare.formplayer.sandbox.SqlSandboxUtils;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.services.FormplayerLockRegistry;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.sqlitedb.UserDB;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.web.client.WebClient;
import org.commcare.formplayer.junit.RestoreFactoryExtension;
import org.commcare.formplayer.junit.request.SyncDbRequest;
import org.commcare.formplayer.junit.request.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
@WebMvcTest
@Import({UtilController.class})
@ContextConfiguration(classes = {TestContext.class, CacheConfiguration.class})
@ExtendWith(InitializeStaticsExtension.class)
public class FilterTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestoreFactory restoreFactoryMock;

    @MockBean
    private FormplayerLockRegistry lockRegistry;

    @RegisterExtension
    static RestoreFactoryExtension restoreFactoryExt = new RestoreFactoryExtension.builder()
            .withUser("test").withDomain("test")
            .withRestorePath("test_restore.xml")
            .build();

    @Test
    public void testSyncDb() throws Exception {
        SyncDbRequest syncDbRequest = new SyncDbRequest(mockMvc, restoreFactoryMock);
        Response response = syncDbRequest.request();
        response.andExpect(jsonPath("status").value("accepted"));

        assert (SqlSandboxUtils.databaseFolderExists(SQLiteProperties.getDataDir()));
        UserDB customConnector = new UserDB("test", "test", null);
        UserSqlSandbox sandbox = new UserSqlSandbox(customConnector);

        SqlStorage<Case> caseStorage = sandbox.getCaseStorage();

        assert (15 == caseStorage.getNumRecords());

        // TODO add ledgers, fixtures, etc.
    }

    @Test
    public void testIntervalSyncDb() throws Exception {
        configureRestoreFactory("synctestdomain", "synctestuser");

        SyncDbResponseBean syncDbResponseBean = intervalSyncDB("synctest", "synctestuser");

        assert (syncDbResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE));
        assert (SqlSandboxUtils.databaseFolderExists(SQLiteProperties.getDataDir()));
    }
}
