package tests;

import application.SQLiteProperties;
import beans.SyncDbResponseBean;
import org.commcare.cases.model.Case;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import sandbox.SqlSandboxUtils;
import sandbox.SqliteIndexedStorageUtility;
import sandbox.UserSqlSandbox;
import sqlitedb.UserDB;
import util.Constants;
import utils.TestContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class FilterTests extends BaseTestClass {

    @Test
    public void testSyncDb() throws Exception {

        configureRestoreFactory("synctestdomain", "synctestuser");

        SyncDbResponseBean syncDbResponseBean = syncDb();

        assert(syncDbResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE));
        assert(SqlSandboxUtils.databaseFolderExists(SQLiteProperties.getDataDir()));

        UserSqlSandbox sandbox = new UserSqlSandbox(new UserDB("synctestdomain","synctestuser", null));

        SqliteIndexedStorageUtility<Case> caseStorage =  sandbox.getCaseStorage();

        assert (15 == caseStorage.getNumRecords());

        //TODO add ledgers, fixtures, etc.
    }
}