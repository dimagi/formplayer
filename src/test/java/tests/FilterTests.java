package tests;

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

    @Before
    public void setUp() throws IOException {
        super.setUp();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"));
    }

    @Test
    public void testRestoreFilter() throws Exception {

        String[] caseArray;

        CaseFilterResponseBean caseFilterResponseBean = filterCases("requests/filter/filter_cases.json");
        caseArray = caseFilterResponseBean.getCases();
        assert(caseArray.length == 3);
        assert(caseArray[0].equals("2aa41fcf4d8a464b82b171a39959ccec"));

        assert(filterCases("requests/filter/filter_cases_2.json").getCases().length == 9);

        caseArray = filterCases("requests/filter/filter_cases_3.json").getCases();
        assert(caseArray.length == 1);
        assert(caseArray[0].equals("e7ed3658d7394415a4bba5edc7055f1d"));

        assert(filterCases("requests/filter/filter_cases_4.json").getCases().length == 15);
    }

    @Test
    public void testSyncDb() throws Exception {

        SyncDbResponseBean syncDbResponseBean = syncDb();

        assert(syncDbResponseBean.getStatus().equals(Constants.RESPONSE_STATUS_POSITIVE));
        assert(SqlSandboxUtils.databaseFolderExists(UserSqlSandbox.DEFAULT_DATBASE_PATH));

        UserSqlSandbox sandbox = SqlSandboxUtils.getStaticStorage("synctestuser", "dbs/synctestdomain");

        SqliteIndexedStorageUtility<Case> caseStorage =  sandbox.getCaseStorage();

        System.out.println("Case Storage records: " + caseStorage.getNumRecords());

        assert(15 == caseStorage.getNumRecords());

        //TODO add ledgers, fixtures, etc.
    }

    @Test
    public void testGetFullCase() throws Exception {
        CaseFilterFullResponseBean caseFilterResponseBean = filterCasesFull();
    }
}