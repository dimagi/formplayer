package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Multimap;

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

import java.util.Hashtable;

/**
 * Regression tests for fixed behaviors
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class CaseClaimTests extends BaseTestClass {

    @Autowired
    CacheManager cacheManager;

    @Captor
    ArgumentCaptor<String> urlCaptor;

    @Captor
    ArgumentCaptor<Multimap<String, String>> requestDataCaptor;

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
    public void testEmptySearch() throws Exception {
        configureQueryMock();

        // When no queryData, Formplayer should return the default values
        QueryResponseBean queryResponseBean = sessionNavigateWithQuery(
                new String[]{"1", "action 1"},
                "caseclaim",
                null,
                true,
                QueryResponseBean.class);
        assert queryResponseBean.getDisplays()[0].getValue().contentEquals("Formplayer");
        assert queryResponseBean.getDisplays()[1].getValue().contentEquals("0");
        assert queryResponseBean.getDisplays()[2].getValue() == null;


        // Empty query data should set all values as null
        Hashtable<String, String> inputs = new Hashtable<>();
        QueryData queryData = new QueryData();
        queryData.setInputs("search_command.m1", inputs);
        queryData.setExecute("search_command.m1", false);
        queryResponseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
                true,
                QueryResponseBean.class);
        assert queryResponseBean.getDisplays()[0].getValue() == null;
        assert queryResponseBean.getDisplays()[1].getValue() == null;
        assert queryResponseBean.getDisplays()[2].getValue() == null;


        // Empty values in query Data should be propogated back as it is from Formplayer
        inputs.put("name", "");
        inputs.put("state", "");
        queryResponseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
                true,
                QueryResponseBean.class);
        assert queryResponseBean.getDisplays()[0].getValue().contentEquals("");
        assert queryResponseBean.getDisplays()[1].getValue().contentEquals("");
        assert queryResponseBean.getDisplays()[2].getValue() == null;

        // Empty params should be carried over to url as well
        queryData.setExecute("search_command.m1", true);
        sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
                true,
                EntityListResponse.class);
        verify(webClientMock, times(1)).postFormData(urlCaptor.capture(),
                requestDataCaptor.capture());
        assertEquals("http://localhost:8000/a/test/phone/search/", urlCaptor.getAllValues().get(0));
        Multimap<String, String> requestData = requestDataCaptor.getAllValues().get(0);
        assertEquals(4, requestData.keySet().size());
        assertArrayEquals(new String[]{"case1", "case2", "case3"},
                requestData.get("case_type").toArray());
        assertArrayEquals(new String[]{""}, requestData.get("name").toArray());
        assertArrayEquals(new String[]{""}, requestData.get("state").toArray());
        assertArrayEquals(new String[]{"False"}, requestData.get("include_closed").toArray());

        // select empty with a valid choice
        inputs.put("name", "#,#chris");
        inputs.put("state", "0");
        inputs.put("district", "#,#1");
        queryData.setExecute("search_command.m1", false);
        queryResponseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
                true,
                QueryResponseBean.class);
        assert queryResponseBean.getDisplays()[0].getValue().contentEquals("#,#chris");
        assert queryResponseBean.getDisplays()[1].getValue().contentEquals("0");
        assert queryResponseBean.getDisplays()[2].getValue().contentEquals("#,#1");

        queryData.setExecute("search_command.m1", true);
        sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
                true,
                EntityListResponse.class);
        verify(webClientMock, times(2)).postFormData(urlCaptor.capture(),
                requestDataCaptor.capture());
        assertEquals("http://localhost:8000/a/test/phone/search/", urlCaptor.getAllValues().get(2));
        requestData = requestDataCaptor.getAllValues().get(2);
        assertEquals(5, requestData.keySet().size());
        assertArrayEquals(new String[]{"case1", "case2", "case3"},
                requestData.get("case_type").toArray());
        assertArrayEquals(new String[]{"", "chris"}, requestData.get("name").toArray());
        assertArrayEquals(new String[]{"", "hampi"}, requestData.get("district").toArray());
        assertArrayEquals(new String[]{"ka"}, requestData.get("state").toArray());
        assertArrayEquals(new String[]{"False"}, requestData.get("include_closed").toArray());
    }

    @Test
    public void testQueryScreen() throws Exception {
        UserSqlSandbox sandbox = new UserSqlSandbox(
                getUserDbConnector("caseclaimdomain", "caseclaimusername", null));
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
                .get("caseclaimdomain_caseclaimusername_http://localhost:8000/a/test/phone/search"
                        + "/_case_type=case1=case2=case3_include_closed=False")
                != null;

        assert responseBean.getEntities().length == 1;
        assert responseBean.getEntities()[0].getId().equals("0156fa3e-093e-4136-b95c-01b13dae66c6");

        // forceManualAction true when default Search on should result in query screen
        QueryResponseBean queryResponseBean = sessionNavigateWithQuery(
                new String[]{"1", "action 1"},
                "caseclaim",
                null,
                true,
                QueryResponseBean.class);
        assert queryResponseBean.getDisplays().length == 3;
        // test default value
        assert queryResponseBean.getDisplays()[0].getValue().contentEquals("Formplayer");
        assert !queryResponseBean.getDisplays()[0].isAllowBlankValue();
        assertArrayEquals(queryResponseBean.getDisplays()[1].getItemsetChoices(),
                new String[]{"karnataka", "Raj as than"});
        assert queryResponseBean.getDisplays()[1].getValue().contentEquals("0");
        assert queryResponseBean.getDisplays()[1].isAllowBlankValue();
        assertArrayEquals(queryResponseBean.getDisplays()[2].getItemsetChoices(),
                new String[]{"Bangalore", "Hampi"});
        assert !queryResponseBean.getDisplays()[2].isAllowBlankValue();

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

        // no value in queryDictionary should reset the value to null
        assert queryResponseBean.getDisplays()[0].getValue() == null;
        assert queryResponseBean.getDisplays()[1].getValue().contentEquals("1");
        assert queryResponseBean.getDisplays()[2].getValue() == null;
        assertArrayEquals(queryResponseBean.getDisplays()[1].getItemsetChoices(),
                new String[]{"karnataka", "Raj as than"});
        assertArrayEquals(queryResponseBean.getDisplays()[2].getItemsetChoices(),
                new String[]{"Baran", "Kota"});


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
        assertArrayEquals(queryResponseBean.getDisplays()[1].getItemsetChoices(),
                new String[]{"karnataka", "Raj as than"});

        // check if we have districts corresponding to karnataka state
        assertArrayEquals(queryResponseBean.getDisplays()[2].getItemsetChoices(),
                new String[]{"Bangalore", "Hampi"});


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
        assert caseStorage.getNumRecords() == 21;

        // When we sync afterwards, include new case and case-claim
        RestoreFactoryAnswer answer = new RestoreFactoryAnswer("restores/caseclaim2.xml");
        Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml(anyBoolean());

        CommandListResponseBean commandResponse = sessionNavigateWithQuery(
                new String[]{"1", "action 1", "0156fa3e-093e-4136-b95c-01b13dae66c6"},
                "caseclaim",
                queryData,
                true,
                CommandListResponseBean.class);
        assert commandResponse.getCommands().length == 2;
        assert commandResponse.getSelections().length == 2;
        assert commandResponse.getSelections()[1].equals("0156fa3e-093e-4136-b95c-01b13dae66c6");
        assert caseStorage.getNumRecords() == 23;

        verify(webClientMock, times(2)).postFormData(urlCaptor.capture(),
                requestDataCaptor.capture());

        // when default search, prompts doesn't get included
        assertEquals("http://localhost:8000/a/test/phone/search/", urlCaptor.getAllValues().get(0));
        Multimap<String, String> requestData = requestDataCaptor.getAllValues().get(0);
        assertEquals(2, requestData.keySet().size());
        assertArrayEquals(new String[]{"case1", "case2", "case3"},
                requestData.get("case_type").toArray());
        assertArrayEquals(new String[]{"False"}, requestData.get("include_closed").toArray());

        // when default search but forceManualSearch, prompts should get included
        // Subsequently when search happens as part of replaying a session, prompts should be
        // same as the last search
        // and therefore be served through cache. Therefore there are only 2 http calls here
        // instead of 3
        assertEquals("http://localhost:8000/a/test/phone/search/", urlCaptor.getAllValues().get(1));
        requestData = requestDataCaptor.getAllValues().get(1);
        assertEquals(5, requestData.keySet().size());
        assertArrayEquals(new String[]{"case1", "case2", "case3"},
                requestData.get("case_type").toArray());
        assertArrayEquals(new String[]{"Burt"}, requestData.get("name").toArray());
        assertArrayEquals(new String[]{"bang", "hampi"}, requestData.get("district").toArray());
        assertArrayEquals(new String[]{"ka"}, requestData.get("state").toArray());
        assertArrayEquals(new String[]{"False"}, requestData.get("include_closed").toArray());
    }

    @Test
    public void testAlreadyOwnCase() throws Exception {
        Hashtable<String, String> inputs = new Hashtable<>();
        inputs.put("name", "Burt");
        QueryData queryData = new QueryData();
        queryData.setExecute("search_command.m1", true);
        queryData.setInputs("search_command.m1", inputs);

        configureQueryMockOwned();
        configureSyncMock();
        RestoreFactoryAnswer answer = new RestoreFactoryAnswer("restores/caseclaim.xml");
        Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml(anyBoolean());

        CommandListResponseBean response = sessionNavigateWithQuery(
                new String[]{"1", "action 1", "3512eb7c-7a58-4a95-beda-205eb0d7f163"},
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
        when(webClientMock.postFormData(anyString(), any(Multimap.class)))
                .thenReturn(FileUtils.getFile(this.getClass(),
                        "query_responses/case_claim_response.xml"));
    }

    private void configureQueryMockOwned() {
        when(webClientMock.postFormData(anyString(), any(Multimap.class)))
                .thenReturn(FileUtils.getFile(this.getClass(),
                        "query_responses/case_claim_response_owned.xml"));
    }
}
