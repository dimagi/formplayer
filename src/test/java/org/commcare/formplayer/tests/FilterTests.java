package org.commcare.formplayer.tests;

import org.commcare.cases.model.Case;
import org.commcare.formplayer.application.SQLiteProperties;
import org.commcare.formplayer.beans.SyncDbResponseBean;
import org.commcare.formplayer.sandbox.SqlSandboxUtils;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class FilterTests extends BaseTestClass {

    @Test
    public void testSyncDb() throws Exception {

        configureRestoreFactory("synctestdomain", "synctestuser");

        SyncDbResponseBean syncDbResponseBean = syncDb();

        assert (syncDbResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE));
        assert (SqlSandboxUtils.databaseFolderExists(SQLiteProperties.getDataDir()));

        UserSqlSandbox sandbox = new UserSqlSandbox(getUserDbConnector("synctestdomain", "synctestuser", null));

        SqlStorage<Case> caseStorage = sandbox.getCaseStorage();

        assert (15 == caseStorage.getNumRecords());

        //TODO add ledgers, fixtures, etc.
    }
}
