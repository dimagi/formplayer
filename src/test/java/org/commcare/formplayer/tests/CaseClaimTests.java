package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
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
import org.commcare.formplayer.junit.RestoreFactoryAnswer;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.suite.model.QueryPrompt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import java.util.Hashtable;

import javax.annotation.Nullable;

/**
 * Regression tests for fixed behaviors
 */
@WebMvcTest
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
    public void testCaseSearchCacheExists() {
        Cache cache = cacheManager.getCache("case_search");
        assertNotNull(cache);
        assertTrue(cache instanceof CaffeineCache);
    }

    @Test
    public void testEmptySearch() throws Exception {
        configureQueryMock();
        QueryData queryData = new QueryData();
        queryData.setForceManualSearch("search_command.m1", true);
        // When no queryData, Formplayer should return the default values
        QueryResponseBean queryResponseBean = runQuery(queryData);
        assert queryResponseBean.getDisplays()[0].getValue().contentEquals("Formplayer");
        assert queryResponseBean.getDisplays()[1].getValue().contentEquals("0");
        assert queryResponseBean.getDisplays()[2].getValue() == null;


        // Empty query data should set all values as null
        Hashtable<String, String> inputs = new Hashtable<>();
        queryData = setUpQueryDataWithInput(inputs, true, false);
        queryResponseBean = runQuery(queryData);
        assert queryResponseBean.getDisplays()[0].getValue() == null;
        assert queryResponseBean.getDisplays()[1].getValue() == null;
        assert queryResponseBean.getDisplays()[2].getValue() == null;


        // Empty values in query Data should be propogated back as it is from Formplayer
        inputs.put("name", "");
        inputs.put("state", "");
        queryResponseBean = runQuery(queryData);
        assert queryResponseBean.getDisplays()[0].getValue().contentEquals("");
        assert queryResponseBean.getDisplays()[1].getValue().contentEquals("");
        assert queryResponseBean.getDisplays()[2].getValue() == null;

        // Empty params should be carried over to url as well
        queryData.setExecute("search_command.m1", true);
        sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
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
        queryResponseBean = runQuery(queryData);
        assert queryResponseBean.getDisplays()[0].getValue().contentEquals("#,#chris");
        assert queryResponseBean.getDisplays()[1].getValue().contentEquals("0");
        assert queryResponseBean.getDisplays()[2].getValue().contentEquals("#,#1");

        queryData.setExecute("search_command.m1", true);
        sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
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
                EntityListResponse.class);

        assert cacheManager.getCache("case_search")
                .get("caseclaimdomain_caseclaimusername_http://localhost:8000/a/test/phone/search"
                        + "/_case_type=case1=case2=case3_include_closed=False")
                != null;

        assert responseBean.getEntities().length == 1;
        assert responseBean.getEntities()[0].getId().equals("0156fa3e-093e-4136-b95c-01b13dae66c6");
        QueryData queryData = new QueryData();
        queryData.setForceManualSearch("search_command.m1", true);

        // forceManualAction true when default Search on should result in query screen
        QueryResponseBean queryResponseBean = runQuery(queryData);
        assert queryResponseBean.getDisplays().length == 5;

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
        queryData = setUpQueryDataWithInput(inputs, true, false);
        queryResponseBean = runQuery(queryData);

        // no value in queryDictionary should reset the value to null
        assert queryResponseBean.getDisplays()[0].getValue() == null;
        assert queryResponseBean.getDisplays()[2].getValue() == null;

        // change selection
        inputs.put("name", "Burt");
        queryResponseBean = runQuery(queryData);
        assert queryResponseBean.getDisplays()[0].getValue().contentEquals("Burt");

        // multi-select test
        inputs.put("state", "0");
        inputs.put("district", "0#,#1"); // select 2 districts
        queryResponseBean = runQuery(queryData);
        assert queryResponseBean.getDisplays()[2].getValue().contentEquals("0#,#1");

        // Select an invalid choice in multi-select and verify it's removed from formplayer response
        inputs.put("district", "0#,#2#,#1");
        queryResponseBean = runQuery(queryData);
        assert queryResponseBean.getDisplays()[2].getValue().contentEquals("0#,#1");

        // Execute Search to get results
        queryData.setExecute("search_command.m1", true);
        responseBean = sessionNavigateWithQuery(new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
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
        // same as the last search and therefore be served through cache.
        // Therefore there are only 2 http calls here instead of 3
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
    public void testQueryPromptRequired() throws Exception {
        QueryData queryData = new QueryData();
        queryData.setForceManualSearch("search_command.m1", true);

        // forceManualAction true when default Search on should result in query screen
        QueryResponseBean queryResponseBean = sessionNavigateWithQuery(
                new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
                QueryResponseBean.class);

        // test old required attr

        assertTrue(queryResponseBean.getDisplays()[0].isRequired());
        assertEquals(queryResponseBean.getDisplays()[0].getRequiredMsg(), QueryPrompt.DEFAULT_REQUIRED_ERROR);
        assertTrue(queryResponseBean.getDisplays()[1].isRequired());
        assertEquals(queryResponseBean.getDisplays()[1].getRequiredMsg(), QueryPrompt.DEFAULT_REQUIRED_ERROR);
        assertFalse(queryResponseBean.getDisplays()[2].isRequired());
        assertEquals(queryResponseBean.getDisplays()[2].getRequiredMsg(), QueryPrompt.DEFAULT_REQUIRED_ERROR);

        // test new required node, both age and dob is required without any input
        String expectedMessage = "One of age or DOB is required";
        assertTrue(queryResponseBean.getDisplays()[3].isRequired());
        assertTrue(queryResponseBean.getDisplays()[3].getRequiredMsg().contentEquals(expectedMessage));
        assertTrue(queryResponseBean.getDisplays()[4].isRequired());
        assertTrue(queryResponseBean.getDisplays()[4].getRequiredMsg().contentEquals(expectedMessage));

        // dynamic condition, inputting age should make dob not required
        Hashtable<String, String> inputs = new Hashtable<>();
        inputs.put("age", "12");
        queryData.setInputs("search_command.m1", inputs);
        queryResponseBean = sessionNavigateWithQuery(
                new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
                QueryResponseBean.class);
        assertFalse(queryResponseBean.getDisplays()[4].isRequired());
        assertTrue(queryResponseBean.getDisplays()[4].getRequiredMsg().contentEquals(expectedMessage));
    }

    public void testDependentItemsets_DependentChoicesChangeWithSelection() throws Exception {
        Hashtable<String, String> inputs = new Hashtable<>();
        inputs.put("state", "1");
        QueryData queryData = setUpQueryDataWithInput(inputs, true, false);
        QueryResponseBean queryResponseBean = runQuery(queryData);
        assert queryResponseBean.getDisplays()[1].getValue().contentEquals("1");
        assertArrayEquals(queryResponseBean.getDisplays()[1].getItemsetChoices(),
                new String[]{"karnataka", "Raj as than"});
        assertArrayEquals(queryResponseBean.getDisplays()[2].getItemsetChoices(),
                new String[]{"Baran", "Kota"});

        inputs.put("state", "0");
        queryResponseBean = runQuery(queryData);
        assert queryResponseBean.getDisplays()[1].getValue().contentEquals("0");
        assertArrayEquals(queryResponseBean.getDisplays()[1].getItemsetChoices(),
                new String[]{"karnataka", "Raj as than"});
        // check if we have districts corresponding to karnataka state
        assertArrayEquals(queryResponseBean.getDisplays()[2].getItemsetChoices(),
                new String[]{"Bangalore", "Hampi"});
    }

    @Test
    public void testDependentItemsets_SelectionPeristsInResponse() throws Exception {
        Hashtable<String, String> inputs = new Hashtable<>();
        inputs.put("state", "0");
        inputs.put("district", "0");
        QueryData queryData = setUpQueryDataWithInput(inputs, true, false);
        QueryResponseBean queryResponseBean = runQuery(queryData);
        assertEquals("0", queryResponseBean.getDisplays()[1].getValue());
        assertEquals("0", queryResponseBean.getDisplays()[2].getValue());

        // Change selection
        inputs.put("state", "1");
        inputs.put("district", "1");
        queryResponseBean = runQuery(queryData);
        assertEquals("1", queryResponseBean.getDisplays()[1].getValue());
        assertEquals("1", queryResponseBean.getDisplays()[2].getValue());
    }

    @Test
    public void testQueryPromptValidation_NullInputCausesNoError() throws Exception {
        runRequestAndValidateAgeError(null, null, true, false);
    }

    @Test
    public void testQueryPromptValidation_EmptyInputCausesNoError() throws Exception {
        runRequestAndValidateAgeError("", null, true, false);
    }

    @Test
    public void testQueryPromptValidation_InvalidInputCausesError() throws Exception {
        runRequestAndValidateAgeError("12", "age should be greater than 18", true, false);
    }

    @Test
    public void testQueryPromptValidation_ValidInputCausesNoError() throws Exception {
        runRequestAndValidateAgeError("21", null, true, false);
    }

    @Test
    public void testQueryPromptValidationWithExecute_NullInputCausesNoError() throws Exception {
        runRequestAndValidateAgeError(null, null, true, true);
    }

    @Test
    public void testQueryPromptValidationWithExecute_EmptyInputCausesNoError() throws Exception {
        runRequestAndValidateAgeError("", null, true, true);
    }

    @Test
    public void testQueryPromptValidationWithExecute_InvalidInputCausesError() throws Exception {
        runRequestAndValidateAgeError("12", "age should be greater than 18", true, true);
    }

    @Test
    public void testQueryPromptValidationWithExecute_ValidInputCausesNoError() throws Exception {
        runRequestAndValidateAgeError("21", null, true, true);
    }

    @Test
    public void testQueryPromptValidation_DefaultSearchCausesNoError() throws Exception {
        QueryData queryData = setUpQueryDataWithAge("12", false, false);
        configureQueryMock();
        try {
            sessionNavigateWithQuery(
                    new String[]{"1", "action 1"},
                    "caseclaim",
                    queryData,
                    EntityListResponse.class);
        } catch (Exception e) {
            fail("Default search failed to proceed to search results without errors", e);
        }
    }

    private void runRequestAndValidateAgeError(String age, @Nullable String expectedError, boolean forceManual,
            boolean execute) throws Exception {
        QueryData queryData = setUpQueryDataWithAge(age, forceManual, execute);
        QueryResponseBean queryResponseBean = runQuery(queryData);
        assertEquals(expectedError, queryResponseBean.getDisplays()[3].getError());
    }

    private QueryResponseBean runQuery(QueryData queryData) throws Exception {
        return sessionNavigateWithQuery(
                new String[]{"1", "action 1"},
                "caseclaim",
                queryData,
                QueryResponseBean.class);
    }

    private QueryData setUpQueryDataWithAge(String age, boolean forceManual, boolean execute) {
        Hashtable<String, String> inputs = new Hashtable<>();
        if (age != null) {
            inputs.put("age", age);
        }
        return setUpQueryDataWithInput(inputs, forceManual, execute);
    }

    private QueryData setUpQueryDataWithInput(Hashtable<String, String> inputs, boolean forceManual,
            boolean execute) {
        QueryData queryData = new QueryData();
        queryData.setInputs("search_command.m1", inputs);
        if (forceManual) {
            queryData.setForceManualSearch("search_command.m1", true);
        }
        if (execute) {
            queryData.setExecute("search_command.m1", true);
        }
        return queryData;
    }

    @Test
    public void testAlreadyOwnCase() throws Exception {
        configureQueryMockOwned();
        configureSyncMock();
        RestoreFactoryAnswer answer = new RestoreFactoryAnswer("restores/caseclaim.xml");
        Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml(anyBoolean());

        Hashtable<String, String> inputs = new Hashtable<>();
        inputs.put("name", "Burt");
        QueryData queryData = setUpQueryDataWithInput(inputs, true, true);
        CommandListResponseBean response = sessionNavigateWithQuery(
                new String[]{"1", "action 1", "3512eb7c-7a58-4a95-beda-205eb0d7f163"},
                "caseclaim",
                queryData,
                CommandListResponseBean.class);
        assert response.getSelections().length == 2;
    }

    private void configureSyncMock() {
        when(webClientMock.caseClaimPost(anyString(), any()))
                .thenReturn(true);
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
