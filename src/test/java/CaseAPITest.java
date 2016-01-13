import hq.CaseAPIs;
import hq.RestoreUtils;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import requests.FilterRequest;
import org.apache.commons.io.IOUtils;
import java.io.IOException;

public class CaseAPITest {

    @Before
    public void setUp() {
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
    }

    @Test
    public void testRestoreFilter() throws Exception {
        String restorePayload = getFile("test_restore.xml");
        String filterRequestPayload = getFile("requests/filter/filter_cases.json");

        FilterRequest filterRequest = new FilterRequest(filterRequestPayload);
        RestoreUtils.restoreUser(filterRequest, restorePayload);

        requestAssert("requests/filter/filter_cases.json", 3);
        requestAssert("requests/filter/filter_cases_2.json", 9);
        requestAssert("requests/filter/filter_cases_3.json", 1);

    }

    public void requestAssert(String filepath, int count) throws Exception{
        String filterRequestPayload = getFile(filepath);
        FilterRequest filterRequest = new FilterRequest(filterRequestPayload);
        String filtered = CaseAPIs.filterCases(filterRequest);
        String[] caseIds = filtered.split(",");
        assert(caseIds.length == count);
    }

    private String getFile(String fileName){

        String result = "";

        ClassLoader classLoader = getClass().getClassLoader();
        try {
            result = IOUtils.toString(classLoader.getResourceAsStream(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;

    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
    }
}