package org.commcare.formplayer.tests;

import org.commcare.cases.model.Case;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.QueryResponseBean;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for fixed behaviors
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class CaseClaimTests extends BaseTestClass {

    @Autowired
    CacheManager cacheManager;

    @Captor
    ArgumentCaptor<URI> uriCaptor;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("caseclaimdomain", "caseclaimusername");
        cacheManager.getCache("case_search").clear();
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

        // forceManualAction false and default Search ON should result in search results right away
        EntityListResponse responseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                null,
                false,
                EntityListResponse.class);

        assert cacheManager.getCache("case_search")
                .get("caseclaimdomain_caseclaimusername_http://localhost:8000/a/test/phone/search/?include_closed=False&case_type=case") != null;

        assert responseBean.getEntities().length == 1;
        assert responseBean.getEntities()[0].getId().equals("0156fa3e-093e-4136-b95c-01b13dae66c6");

        // forceManualAction true when default Search on should result in query screen
        QueryResponseBean queryResponseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                null,
                true,
                QueryResponseBean.class);
        assert queryResponseBean.getDisplays().length == 3;
        // test default value
        assert queryResponseBean.getDisplays()[0].getValue().contentEquals("Formplayer");
        assertArrayEquals(queryResponseBean.getDisplays()[1].getItemsetChoices(), new String[]{"karnataka", "Raj as than"});
        assert queryResponseBean.getDisplays()[1].getValue().contentEquals("0");
        assertArrayEquals(queryResponseBean.getDisplays()[2].getItemsetChoices(), new String[]{"Bangalore", "Hampi"});

        // test hint
        assert queryResponseBean.getDisplays()[1].getHint().contentEquals("This is a hint");

        Hashtable<String, String> inputs = new Hashtable<>();
        inputs.put("state", "1");
        QueryData queryData = new QueryData();
        queryData.setExecute("search_command.m1", false);
        queryData.setInputs("search_command.m1", inputs);
        queryResponseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
                true,
                QueryResponseBean.class);

        // no value in queryDictionary should reset the value to empty
        assert queryResponseBean.getDisplays()[0].getValue().contentEquals("");
        assert queryResponseBean.getDisplays()[1].getValue().contentEquals("1");
        assert queryResponseBean.getDisplays()[2].getValue().contentEquals("");
        assertArrayEquals(queryResponseBean.getDisplays()[1].getItemsetChoices(), new String[]{"karnataka", "Raj as than"});
        assertArrayEquals(queryResponseBean.getDisplays()[2].getItemsetChoices(), new String[]{"Baran", "Kota"});


        // change selection
        inputs.put("name", "Burt");
        inputs.put("state", "0");
        queryData.setInputs("search_command.m1", inputs);
        queryResponseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
                true,
                QueryResponseBean.class);
        assert queryResponseBean.getDisplays()[0].getValue().contentEquals("Burt");
        assert queryResponseBean.getDisplays()[1].getValue().contentEquals("0");
        assertArrayEquals(queryResponseBean.getDisplays()[1].getItemsetChoices(), new String[]{"karnataka", "Raj as than"});

        // check if we have districts corresponding to karnataka state
        assertArrayEquals(queryResponseBean.getDisplays()[2].getItemsetChoices(), new String[]{"Bangalore", "Hampi"});


        // multi-select test
        inputs.put("district", "0#,#1"); // select 2 districts
        queryResponseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
                true,
                QueryResponseBean.class);
        assert queryResponseBean.getDisplays()[2].getValue().contentEquals("0#,#1");

        // Select an invalid choice in multi-select and verify it's removed from formplayer response
        inputs.put("district", "0#,#2#,#1");
        queryResponseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
                true,
                QueryResponseBean.class);
        assert queryResponseBean.getDisplays()[2].getValue().contentEquals("0#,#1");


        queryData.setExecute("search_command.m1", true);
        responseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
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
                queryData,
                true,
                CommandListResponseBean.class);
        assert commandResponse.getCommands().length == 2;
        assert commandResponse.getSelections().length == 2;
        assert commandResponse.getSelections()[1].equals("0156fa3e-093e-4136-b95c-01b13dae66c6");
        assert caseStorage.getNumRecords() == 22;

        // verify search uris
        verify(webClientMock, times(2)).get(uriCaptor.capture());
        List<URI> uris = uriCaptor.getAllValues();
        // when default search, prompts doesn't get included
        assert uris.get(0).equals(new URI("http://localhost:8000/a/test/phone/search/?include_closed=False&case_type=case"));
        // when default search but forceManualSearch, prompts should get included
        // Subsequently when search happens as part of replaying a session, prompts should be same as the last search
        // and therefore be served through cache. Therefore there are only 2 http calls here instead of 3
        assert uris.get(1).equals(new URI("http://localhost:8000/a/test/phone/search/?include_closed=False&name=Burt&case_type=case&state=ka&district=bang&district=hampi"));
    }

    @Test
    public void testAlreadyOwnCase() throws Exception {

        UserSqlSandbox sandbox = new UserSqlSandbox(getUserDbConnector("caseclaimdomain", "caseclaimusername", null));
        SqlStorage<Case> caseStorage = sandbox.getCaseStorage();
        Hashtable<String, String> inputs = new Hashtable<>();
        inputs.put("name", "Burt");
        QueryData queryData = new QueryData();
        queryData.setExecute("search_command.m1", true);
        queryData.setInputs("search_command.m1", inputs);

        configureQueryMockOwned();
        configureSyncMock();
        RestoreFactoryAnswer answer = new RestoreFactoryAnswer("restores/caseclaim.xml");
        Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml(anyBoolean());

        CommandListResponseBean response = sessionNavigateWithQuery(new String[]{"1", "action 1", "3512eb7c-7a58-4a95-beda-205eb0d7f163"},
                "caseclaim",
                queryData,
                true,
                CommandListResponseBean.class);
        assert response.getSelections().length == 2;
    }

    private void configureSyncMock() {
        when(webClientMock.post(anyString(), any()))
                .thenReturn("");
    }

    private void configureQueryMock() {
        when(webClientMock.get(any(URI.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "query_responses/case_claim_response.xml"));
    }

    private void configureQueryMockOwned() {
        when(webClientMock.get(any(URI.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "query_responses/case_claim_response_owned.xml"));
    }
}
