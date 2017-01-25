package tests;

import application.SQLiteProperties;
import auth.HqAuth;
import beans.*;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.cases.model.Case;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import util.Constants;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class FilterTests extends BaseTestClass {

    @Test
    public void testSyncDb() throws Exception {

        configureRestoreFactory("synctestdomain", "synctestuser");

        SyncDbResponseBean syncDbResponseBean = syncDb();

        assert(syncDbResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE));
        assert(SqlSandboxUtils.databaseFolderExists(SQLiteProperties.getDataDir()));

        UserSqlSandbox sandbox = SqlSandboxUtils.getStaticStorage("synctestuser", SQLiteProperties.getDataDir() + "synctestdomain");

        SqliteIndexedStorageUtility<Case> caseStorage =  sandbox.getCaseStorage();

        assert(15 == caseStorage.getNumRecords());

        //TODO add ledgers, fixtures, etc.
    }
}