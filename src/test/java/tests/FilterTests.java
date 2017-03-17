package tests;

import application.SQLiteProperties;
import beans.*;
import sandbox.SqlSandboxUtils;
import sandbox.SqliteIndexedStorageUtility;
import sandbox.UserSqlSandbox;
import org.commcare.cases.model.Case;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import tests.sandbox.TestConnectionHandler;
import util.Constants;
import utils.TestContext;

import static org.mockito.Matchers.any;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class FilterTests extends BaseTestClass {

    @Test
    public void testSyncDb() throws Exception {

        configureRestoreFactory("synctestdomain", "synctestuser");

        SyncDbResponseBean syncDbResponseBean = syncDb();

        assert(syncDbResponseBean.getStatus().equals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE));
        assert(SqlSandboxUtils.databaseFolderExists(SQLiteProperties.getDataDir()));

        UserSqlSandbox sandbox = new UserSqlSandbox(new TestConnectionHandler(SQLiteProperties.getDataDir() + "synctestdomain/synctestuser"));

        SqliteIndexedStorageUtility<Case> caseStorage =  sandbox.getCaseStorage();

        assert (15 == caseStorage.getNumRecords());

        //TODO add ledgers, fixtures, etc.
    }
}