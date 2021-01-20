package org.commcare.formplayer.tests;

import org.commcare.cases.model.Case;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.QueryResponseBean;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Hashtable;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Regression tests for fixed behaviors
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CaseClaimTests extends BaseTestClass {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("caseclaimdomain", "caseclaimusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/caseclaim.xml";
    }

    @Test
    public void testQueryScreen() throws Exception {
        UserSqlSandbox sandbox = new UserSqlSandbox(getUserDbConnector("caseclaimdomain", "caseclaimusername", null));
        SqlStorage<Case> caseStorage = sandbox.getCaseStorage();

        configureQueryMock();
        configureSyncMock();

        QueryResponseBean queryResponseBean = sessionNavigate(new String[]{"1", "action 1"},
                "caseclaim",
                QueryResponseBean.class);
        assert queryResponseBean.getDisplays().length == 3;
        // test default value
        assert queryResponseBean.getDisplays()[0].getValue().contentEquals("Formplayer");
        assertArrayEquals(queryResponseBean.getDisplays()[1].getItemsetChoices(), new String[]{"karnataka", "Raj as than"});
        assert queryResponseBean.getDisplays()[1].getValue().contentEquals("0");
        assertArrayEquals(queryResponseBean.getDisplays()[2].getItemsetChoices(), new String[]{"Bangalore", "Hampi"});

        Hashtable<String, String> queryDictionary = new Hashtable<>();
        queryDictionary.put("state", "1");
        queryResponseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryDictionary,
                false,
                QueryResponseBean.class);

        // no value in queryDictionary should reset the value to empty
        assert queryResponseBean.getDisplays()[0].getValue().contentEquals("");
        assertArrayEquals(queryResponseBean.getDisplays()[1].getItemsetChoices(), new String[]{"karnataka", "Raj as than"});
        assertArrayEquals(queryResponseBean.getDisplays()[2].getItemsetChoices(), new String[]{"Baran", "Kota"});

        // change selection
        queryDictionary.put("name", "Burt");
        queryDictionary.put("state", "0");
        queryResponseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryDictionary,
                false,
                QueryResponseBean.class);
        assert queryResponseBean.getDisplays()[0].getValue().contentEquals("Burt");
        assertArrayEquals(queryResponseBean.getDisplays()[1].getItemsetChoices(), new String[]{"karnataka", "Raj as than"});
        assertArrayEquals(queryResponseBean.getDisplays()[2].getItemsetChoices(), new String[]{"Bangalore", "Hampi"});

        EntityListResponse responseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryDictionary,
                true,
                EntityListResponse.class);

        assert responseBean.getEntities().length == 1;
        assert responseBean.getEntities()[0].getId().equals("0156fa3e-093e-4136-b95c-01b13dae66c6");
        assert caseStorage.getNumRecords() == 20;

        // When we sync afterwards, include new case and case-claim 
        RestoreFactoryAnswer answer = new RestoreFactoryAnswer("restores/caseclaim2.xml");
        Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml(anyBoolean());

        CommandListResponseBean commandResponse = sessionNavigateWithQuery(new String[]{"1", "action 1", "0156fa3e-093e-4136-b95c-01b13dae66c6"},
                "caseclaim",
                queryDictionary,
                true,
                CommandListResponseBean.class);
        assert commandResponse.getCommands().length == 2;
        assert commandResponse.getSelections().length == 2;
        assert commandResponse.getSelections()[1].equals("0156fa3e-093e-4136-b95c-01b13dae66c6");
        assert caseStorage.getNumRecords() == 22;
    }

    @Test
    public void testAlreadyOwnCase() throws Exception {

        UserSqlSandbox sandbox = new UserSqlSandbox(getUserDbConnector("caseclaimdomain", "caseclaimusername", null));
        SqlStorage<Case> caseStorage = sandbox.getCaseStorage();
        Hashtable<String, String> queryDictionary = new Hashtable<>();
        queryDictionary.put("name", "Burt");

        configureQueryMockOwned();
        configureSyncMock();
        RestoreFactoryAnswer answer = new RestoreFactoryAnswer("restores/caseclaim.xml");
        Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml(anyBoolean());

        CommandListResponseBean response = sessionNavigateWithQuery(new String[]{"1", "action 1", "3512eb7c-7a58-4a95-beda-205eb0d7f163"},
                "caseclaim",
                queryDictionary,
                true,
                CommandListResponseBean.class);
        assert response.getSelections().length == 2;
    }

    private void configureSyncMock() {
        when(syncRequester.makeSyncRequest(anyString(), anyString(), any(HttpHeaders.class)))
                .thenReturn(new ResponseEntity<String>(HttpStatus.OK));
    }

    private void configureQueryMock() {
        when(queryRequester.makeQueryRequest(anyString(), any(HttpHeaders.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "query_responses/case_claim_response.xml"));
    }

    private void configureQueryMockOwned() {
        when(queryRequester.makeQueryRequest(anyString(), any(HttpHeaders.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "query_responses/case_claim_response_owned.xml"));
    }
}
