package org.commcare.formplayer.tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Multimap;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.QueryResponseBean;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * Tests Navigation with different case search workflows with Search Parent First(SPF) set to
 * 'Parrent' and 'Other -> Parent`
 */
@WebMvcTest
public class CaseClaimNavigationParentChildTests extends BaseTestClass {

    private static final String PARENT_CASE_ID = "192262cb-fbfa-46a4-ba91-a9d13659b0e0";
    private static final String CHILD_CASE_ID = "d29c08f5-943c-42ca-b0a5-f64cbddba087";

    private final String APP_CASE_CLAIM_SPF_PARENT = "case_claim_spf_parent";
    private final String APP_CASE_CLAIM_SPF_OTHER = "case_claim_spf_other";
    private final String APP_CASE_CLAIM_EOF_NAVIGATION = "case_claim_eof_navigation";


    private final String INDEX_PARENT_SEARCH_FIRST = "1";
    private final String INDEX_PARENT_SEE_MORE = "2";
    private final String INDEX_PARENT_SKIP_TO_RESULTS = "3";

    private final String INDEX_PARENT_SEARCH_FIRST_CHILD = "1";
    private final String INDEX_PARENT_SEARCH_FIRST_CHILD_SEARCH_FIRST = "2";
    private final String INDEX_PARENT_SEARCH_FIRST_CHILD_SEE_MORE = "3";
    private final String INDEX_PARENT_SEARCH_FIRST_CHILD_SKIP_TO_RESULTS = "4";

    private final String INDEX_CHILD_FORM = "0";


    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("caseclaimdomain", "caseclaimusername");
        configureQueryMock();
        configureSyncMock();
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/case_claim_parent_child.xml";
    }

    @Test
    public void testSpfOtherWithSameCaseType_SearchFirst() throws Exception {
        testParentSearchFirst(APP_CASE_CLAIM_SPF_OTHER, PARENT_CASE_ID);
    }

    @Test
    public void testSpfOtherWithSameCaseType_SeeMore() throws Exception {
        testParentSeeMore(APP_CASE_CLAIM_SPF_OTHER, PARENT_CASE_ID);
    }

    @Test
    public void testSpfOtherWithSameCaseType_SkipToSearchResults() throws Exception {
        testParentSkipToSearchResults(APP_CASE_CLAIM_SPF_OTHER, PARENT_CASE_ID);
    }

    @Test
    public void testSpfParentWithChildCase_SearchFirst() throws Exception {
        testParentSearchFirst(APP_CASE_CLAIM_SPF_PARENT, CHILD_CASE_ID);
    }

    @Test
    public void testSpfParentWithChildCase_SeeMore() throws Exception {
        testParentSeeMore(APP_CASE_CLAIM_SPF_PARENT, CHILD_CASE_ID);
    }

    @Test
    public void testSpfParentWithChildCase_SkipToSearchResults() throws Exception {
        testParentSkipToSearchResults(APP_CASE_CLAIM_SPF_PARENT, CHILD_CASE_ID);
    }

    @Test
    public void testEofNavigation() throws Exception {
        when(webClientMock.postFormData(anyString(), argThat(data -> {
            return ((Multimap<String, String>)data).get("case_type").contains("song");
        }))).thenReturn(FileUtils.getFile(this.getClass(),
                "query_responses/case_claim_parent_child_response.xml"));
        String appName = APP_CASE_CLAIM_EOF_NAVIGATION;
        ArrayList<String> selections = new ArrayList<>();
        selections.add("1");
        sessionNavigateWithQuery(selections,
                appName,
                null,
                QueryResponseBean.class);
        // execute search
        QueryData queryData = new QueryData();
        queryData.setExecute("search_command.m1", true);

        Hashtable<String, String> inputs = new Hashtable<>();
        inputs.put("rating", "4");
        queryData.setInputs("search_command.m1", inputs);

        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                appName,
                queryData,
                EntityListResponse.class);

        Assertions.assertEquals(1, entityListResponse.getEntities().length);
        Assertions.assertEquals(PARENT_CASE_ID, entityListResponse.getEntities()[0].getId());

        selections.add(PARENT_CASE_ID);
        sessionNavigateWithQuery(selections,
                appName,
                queryData,
                CommandListResponseBean.class);

        selections.add("2");

        NewFormResponse response = sessionNavigateWithQuery(selections,
                appName,
                queryData,
                NewFormResponse.class);

        SubmitResponseBean submitResponse = submitForm(
                getAnswers("0", "0"),
                response.getSessionId()
        );

        CommandListResponseBean commandResponse = getNextScreenForEofNavigation(submitResponse,
                CommandListResponseBean.class);

        inputs.put("rating", "2");
        queryData.setInputs("search_command.m1", inputs);

        // return search results that doesn't have the selected case
        when(webClientMock.postFormData(any(), argThat(data -> {
            return ((Multimap<String, String>)data).get("case_type").contains("song");
        }))).thenReturn(FileUtils.getFile(this.getClass(),
                "query_responses/case_claim_parent_child_child_response.xml"));

        // since the case claim has happened already, this should not redo the search and trigger
        // the query above
        // If that happens, it would result into an Entity Screen selection error
        sessionNavigateWithQuery(selections,
                appName,
                queryData,
                NewFormResponse.class);
    }


    public void testParentSearchFirst(String appName, String subCaseSelectionId) throws Exception {
        ArrayList<String> selections = new ArrayList<>();
        selections.add(INDEX_PARENT_SEARCH_FIRST);

        // we see a search screen first
        sessionNavigateWithQuery(selections,
                appName,
                null,
                QueryResponseBean.class);

        // execute search
        QueryData queryData = new QueryData();
        queryData.setExecute("search_command.m5", true);

        testParentSearchResults(appName, queryData, selections);
        testParentSelection(appName, queryData, selections);

        testChildSearchNormal(appName, queryData, new ArrayList<>(selections), "search_command.m6",
                subCaseSelectionId);
        testChildSearchFirst(appName, queryData, new ArrayList<>(selections), "search_command.m7",
                subCaseSelectionId);
        testChildSeeMore(appName, queryData, new ArrayList<>(selections), subCaseSelectionId);
        testChildSkipToResults(appName, queryData, new ArrayList<>(selections), subCaseSelectionId);
    }

    public void testParentSeeMore(String appName, String subCaseSelectionId) throws Exception {
        ArrayList<String> selections = new ArrayList<>();
        selections.add(INDEX_PARENT_SEE_MORE);
        QueryData queryData = new QueryData();

        // we should see parent case list with search action
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                APP_CASE_CLAIM_SPF_PARENT,
                queryData,
                EntityListResponse.class);

        assert entityListResponse.getActions().length == 1;

        // move to search screen
        selections.add("action 0");
        sessionNavigateWithQuery(selections,
                APP_CASE_CLAIM_SPF_PARENT,
                queryData,
                EntityListResponse.class);

        queryData.setExecute("search_command.m10", true);
        testParentSearchResults(appName, queryData, selections);
        testParentSelection(appName, queryData, selections);

        testChildSearchFirst(appName, queryData, new ArrayList<>(selections), "search_command.m12",
                subCaseSelectionId);
        testChildSeeMore(appName, queryData, new ArrayList<>(selections), subCaseSelectionId);
        testChildSkipToResults(appName, queryData, new ArrayList<>(selections), subCaseSelectionId);
    }

    public void testParentSkipToSearchResults(String appName, String subCaseSelectionId)
            throws Exception {
        ArrayList<String> selections = new ArrayList<>();
        selections.add(INDEX_PARENT_SKIP_TO_RESULTS);

        // we should move directly to search results
        QueryData queryData = new QueryData();
        testParentSearchResults(appName, queryData, selections);
        testParentSelection(appName, queryData, selections);

        testChildSearchFirst(appName, queryData, new ArrayList<>(selections), "search_command.m17",
                subCaseSelectionId);
        testChildSeeMore(appName, queryData, new ArrayList<>(selections), subCaseSelectionId);
        testChildSkipToResults(appName, queryData, new ArrayList<>(selections), subCaseSelectionId);
        testParentSkipToResultsChildForceManualSearch(appName, queryData,
                new ArrayList<>(selections), "search_command.m18", subCaseSelectionId);
    }

    private void testChildSearchNormal(String appName, QueryData queryData,
            ArrayList<String> selections, String searchKey, String subCaseSelectionId)
            throws Exception {
        selections.add(INDEX_PARENT_SEARCH_FIRST_CHILD);
        sessionNavigateWithQuery(selections,
                appName,
                queryData,
                EntityListResponse.class);

        // clicking search moves user to search screen
        selections.add("action 0");

        sessionNavigateWithQuery(selections,
                appName,
                queryData,
                QueryResponseBean.class);

        queryData.setExecute(searchKey, true);
        testChildSearchResult(appName, queryData, selections, subCaseSelectionId);
        testChildSelection(appName, queryData, selections, subCaseSelectionId);
    }

    private void testParentSearchResults(String appName, QueryData queryData,
            ArrayList<String> selections) throws Exception {
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                appName,
                queryData,
                EntityListResponse.class);

        Assertions.assertEquals(1, entityListResponse.getEntities().length);
        Assertions.assertEquals(PARENT_CASE_ID, entityListResponse.getEntities()[0].getId());
    }

    private void testParentSelection(String appName, QueryData queryData,
            ArrayList<String> selections) throws Exception {
        selections.add(PARENT_CASE_ID);
        CommandListResponseBean commandListResponseBean = sessionNavigateWithQuery(selections,
                appName,
                queryData,
                CommandListResponseBean.class);

        assert commandListResponseBean.getCommands().length == 5;
    }

    private void testChildSearchFirst(String appName, QueryData queryData,
            ArrayList<String> selections, String searchKey,
            String subCaseSelectionId) throws Exception {
        // we move to child search directly
        selections.add(INDEX_PARENT_SEARCH_FIRST_CHILD_SEARCH_FIRST);
        sessionNavigateWithQuery(selections,
                APP_CASE_CLAIM_SPF_PARENT,
                queryData,
                QueryResponseBean.class);

        // Execute child search
        queryData.setExecute(searchKey, true);

        testChildSearchResult(appName, queryData, selections, subCaseSelectionId);
        testChildSelection(appName, queryData, selections, subCaseSelectionId);
    }

    private void testChildSkipToResults(String appName, QueryData queryData,
            ArrayList<String> selections, String subCaseSelectionId) throws Exception {
        // we move to child search results directly
        selections.add(INDEX_PARENT_SEARCH_FIRST_CHILD_SKIP_TO_RESULTS);
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                APP_CASE_CLAIM_SPF_PARENT,
                queryData,
                EntityListResponse.class);
        testChildSearchResult(appName, queryData, selections, subCaseSelectionId);
        testChildSelection(appName, queryData, selections, subCaseSelectionId);
    }

    private void testParentSkipToResultsChildForceManualSearch(String appName, QueryData queryData,
            ArrayList<String> selections, String searchKey, String subCaseSelectionId)
            throws Exception {

        // we see child's case list
        selections.add(INDEX_PARENT_SEARCH_FIRST_CHILD_SEE_MORE);
        sessionNavigateWithQuery(selections,
                appName,
                queryData,
                EntityListResponse.class);

        // click search to show results
        selections.add("action 0");
        queryData.setForceManualSearch(searchKey, true);
        sessionNavigateWithQuery(selections,
                appName,
                queryData,
                QueryResponseBean.class);
    }

    // test result of executing child search
    private void testChildSearchResult(String appName, QueryData queryData,
            ArrayList<String> selections, String subCaseSelectionId) throws Exception {
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                appName,
                queryData,
                EntityListResponse.class);

        Assertions.assertEquals(1, entityListResponse.getEntities().length);
        Assertions.assertEquals(subCaseSelectionId, entityListResponse.getEntities()[0].getId());
    }

    // selecting child should show child update form
    private void testChildSelection(String appName, QueryData queryData,
            ArrayList<String> selections, String subCaseSelectionId) throws Exception {
        selections.add(subCaseSelectionId);
        CommandListResponseBean commandListResponseBean = sessionNavigateWithQuery(selections,
                appName,
                queryData,
                CommandListResponseBean.class);

        Assertions.assertEquals(1, commandListResponseBean.getCommands().length);

        selections.add(INDEX_CHILD_FORM);
        sessionNavigateWithQuery(selections,
                appName,
                queryData,
                NewFormResponse.class);
    }


    private void testChildSeeMore(String appName, QueryData queryData,
            ArrayList<String> selections, String subCaseSelectionId) throws Exception {
        // we see a case list first
        selections.add(INDEX_PARENT_SEARCH_FIRST_CHILD_SEE_MORE);
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                APP_CASE_CLAIM_SPF_PARENT,
                queryData,
                EntityListResponse.class);

        // clicking search moves user to search results screen directly
        selections.add("action 0");

        testChildSearchResult(appName, queryData, selections, subCaseSelectionId);
        testChildSelection(appName, queryData, selections, subCaseSelectionId);
    }

    private void configureSyncMock() {
        when(webClientMock.caseClaimPost(anyString(), any())).thenThrow(
                new RuntimeException("not expecting a claim request"));
    }

    private void configureQueryMock() {
        when(webClientMock.postFormData(anyString(), argThat(data -> {
            return (data != null) &&
                    ((Multimap<String, String>)data).get("case_type").contains("song");
        }))).thenReturn(FileUtils.getFile(this.getClass(),
                "query_responses/case_claim_parent_child_response.xml"));

        when(webClientMock.postFormData(anyString(), argThat(data -> {
            return (data != null) &&
                    ((Multimap<String, String>)data).get("case_type").contains("show");
        }))).thenReturn(FileUtils.getFile(this.getClass(),
                "query_responses/case_claim_parent_child_child_response.xml"));
    }

    private HashMap<String, Object> getAnswers(String index, String answer) {
        HashMap<String, Object> ret = new HashMap<>();
        ret.put(index, answer);
        return ret;
    }
}
