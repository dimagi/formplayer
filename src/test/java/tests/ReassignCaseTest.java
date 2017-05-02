package tests;

import application.SQLiteProperties;
import beans.NewFormResponse;
import beans.SubmitRequestBean;
import beans.menus.CommandListResponseBean;
import beans.menus.EntityListResponse;
import org.commcare.cases.model.Case;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import sandbox.SqliteIndexedStorageUtility;
import sandbox.UserSqlSandbox;
import tests.sandbox.TestConnectionHandler;
import utils.FileUtils;
import utils.TestContext;

import java.util.HashMap;
import java.util.Hashtable;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * Regression tests for fixed behaviors
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class ReassignCaseTest extends BaseTestClass {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("reassigndomain", "reassignusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/reassign.xml";
    }

    @Test
    public void testCaseReassign() throws Exception {

        UserSqlSandbox sandbox = new UserSqlSandbox(new TestConnectionHandler(SQLiteProperties.getDataDir() + "reassigndomain/reassignusername"));
        SqliteIndexedStorageUtility<Case> caseStorage =  sandbox.getCaseStorage();

        assert caseStorage.getNumRecords() == 0;

        NewFormResponse formResponse = sessionNavigate(new String[] {"0"}, "reassign", NewFormResponse.class);
        HashMap <String, Object> answers = new HashMap<>();
        answers.put("1", "Batman");
        submitForm(answers, formResponse.getSessionId());

        assert caseStorage.getNumRecords() == 1;

        EntityListResponse entityListResponse = sessionNavigate(new String[] {"1", "2"}, "reassign", EntityListResponse.class);
        assert entityListResponse.getEntities().length == 1;
        String caseId = entityListResponse.getEntities()[0].getId();

        NewFormResponse formResponse2 = sessionNavigate(new String[] {"1", "2", caseId}, "reassign", NewFormResponse.class);
        HashMap<String, Object> answers2 = new HashMap<>();
        answers2.put("0", "123");
        submitForm(answers2, formResponse2.getSessionId());

        //assert caseStorage.getNumRecords() == 0;

        EntityListResponse entityListResponse2 = sessionNavigate(new String[] {"1", "2"}, "reassign", EntityListResponse.class);
        assert entityListResponse2.getEntities().length == 0;
    }
}
