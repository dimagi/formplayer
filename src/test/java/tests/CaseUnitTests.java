package tests;

import hq.CaseAPIs;
import hq.RestoreUtils;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import requests.FilterRequest;
import org.apache.commons.io.IOUtils;
import utils.FileUtils;

import java.io.File;
import java.io.IOException;

public class CaseUnitTests {

    @Before
    public void setUp() {
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
    }

    @Test
    public void testRestoreFilter() throws Exception {
        String restorePayload = FileUtils.getFile(this.getClass(), "test_restore.xml");
        String filterRequestPayload = FileUtils.getFile(this.getClass(), "requests/filter/filter_cases.json");

        FilterRequest filterRequest = new FilterRequest(filterRequestPayload);
        RestoreUtils.restoreUser(filterRequest, restorePayload);

        requestAssert("requests/filter/filter_cases.json", 3);
        requestAssert("requests/filter/filter_cases_2.json", 9);
        requestAssert("requests/filter/filter_cases_3.json", 1);

    }

    public void requestAssert(String filepath, int count) throws Exception{
        String filterRequestPayload = FileUtils.getFile(this.getClass(), filepath);
        FilterRequest filterRequest = new FilterRequest(filterRequestPayload);
        String filtered = CaseAPIs.filterCases(filterRequest);
        String[] caseIds = filtered.split(",");
        assert(caseIds.length == count);
    }

    @After
    public void tearDown(){
        SqlSandboxUtils.deleteDatabaseFolder(UserSqlSandbox.DEFAULT_DATBASE_PATH);
    }
}