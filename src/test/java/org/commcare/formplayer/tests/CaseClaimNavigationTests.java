package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.QueryResponseBean;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests Navigation with different case search workflows with Search Parent First(SPF)
 * set to 'Parrent' and 'Other -> Parent`
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class CaseClaimNavigationTests extends BaseTestClass {

    private static final String PARENT_CASE_ID = "192262cb-fbfa-46a4-ba91-a9d13659b0e0";
    private static final String CHILD_CASE_ID = "d29c08f5-943c-42ca-b0a5-f64cbddba087";

    private final String APP_CASE_CLAIM_SPF_PARENT = "case_claim_spf_parent";
    private final String APP_CASE_CLAIM_SPF_OTHER = "case_claim_spf_other";
    
    
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
    public void testSpfOtherWithSameCaseType() throws Exception {
        testParentSearchFirst(APP_CASE_CLAIM_SPF_OTHER, PARENT_CASE_ID);
        testParentSeeMore(APP_CASE_CLAIM_SPF_OTHER, PARENT_CASE_ID);
        testParentSkipToSearchResults(APP_CASE_CLAIM_SPF_OTHER, PARENT_CASE_ID);
    }

    @Test
    public void testSpfParentWithChildCase() throws Exception {
        testParentSearchFirst(APP_CASE_CLAIM_SPF_PARENT, CHILD_CASE_ID);
        testParentSeeMore(APP_CASE_CLAIM_SPF_PARENT, CHILD_CASE_ID);
        testParentSkipToSearchResults(APP_CASE_CLAIM_SPF_PARENT, CHILD_CASE_ID);
    }


    public void testParentSearchFirst(String appName, String subCaseSelectionId) throws Exception {
        ArrayList<String> selections = new ArrayList<>();
        selections.add(INDEX_PARENT_SEARCH_FIRST);

        // we see a search screen first
        sessionNavigateWithQuery(selections,
                appName,
                null,
                false,
                QueryResponseBean.class);

        // execute search
        QueryData queryData = new QueryData();
        queryData.setExecute("search_command.m5", true);

        testParentSearchResults(appName, queryData, selections);
        testParentSelection(appName, queryData, selections);

        testChildSearchNormal(appName, queryData, new ArrayList<>(selections), "search_command.m6", subCaseSelectionId);
        testChildSearchFirst(appName,queryData, new ArrayList<>(selections), "search_command.m7", subCaseSelectionId);
        testChildSeeMore(appName,queryData, new ArrayList<>(selections), subCaseSelectionId);
        testChildSkipToResults(appName,queryData, new ArrayList<>(selections), subCaseSelectionId);
    }

    public void testParentSeeMore(String appName, String subCaseSelectionId) throws Exception {
        ArrayList<String> selections = new ArrayList<>();
        selections.add(INDEX_PARENT_SEE_MORE);
        QueryData queryData = new QueryData();

        // we should see parent case list with search action
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                APP_CASE_CLAIM_SPF_PARENT,
                queryData,
                false,
                EntityListResponse.class);

        assert entityListResponse.getActions().length == 1;

        // move to search screen
        selections.add("action 0");
        sessionNavigateWithQuery(selections,
                APP_CASE_CLAIM_SPF_PARENT,
                queryData,
                false,
                EntityListResponse.class);

        queryData.setExecute("search_command.m10", true);
        testParentSearchResults(appName, queryData, selections);
        testParentSelection(appName, queryData, selections);

        testChildSearchFirst(appName,queryData, new ArrayList<>(selections), "search_command.m12",subCaseSelectionId);
        testChildSeeMore(appName,queryData, new ArrayList<>(selections),subCaseSelectionId);
        testChildSkipToResults(appName,queryData, new ArrayList<>(selections),subCaseSelectionId);
    }

    public void testParentSkipToSearchResults(String appName, String subCaseSelectionId) throws Exception {
        ArrayList<String> selections = new ArrayList<>();
        selections.add(INDEX_PARENT_SKIP_TO_RESULTS);

        // we should move directly to search results
        QueryData queryData = new QueryData();
        testParentSearchResults(appName, queryData, selections);
        testParentSelection(appName, queryData, selections);

        testChildSearchFirst(appName,queryData, new ArrayList<>(selections), "search_command.m17",subCaseSelectionId);
        testChildSeeMore(appName,queryData, new ArrayList<>(selections),subCaseSelectionId);
        testChildSkipToResults(appName,queryData, new ArrayList<>(selections),subCaseSelectionId);
    }

    private void testChildSearchNormal(String appName, QueryData queryData,
                                       ArrayList<String> selections, String searchKey, String subCaseSelectionId) throws Exception {
        selections.add(INDEX_PARENT_SEARCH_FIRST_CHILD);
        sessionNavigateWithQuery(selections,
                appName,
                queryData,
                false,
                EntityListResponse.class);

        // clicking search moves user to search screen
        selections.add("action 0");

        sessionNavigateWithQuery(selections,
                appName,
                queryData,
                false,
                QueryResponseBean.class);
        queryData.setExecute(searchKey, true);

        testChildSearchResult(appName, queryData, selections, subCaseSelectionId);
        testChildSelection(appName, queryData, selections, subCaseSelectionId);
    }

    private void testParentSearchResults(String appName, QueryData queryData, ArrayList<String> selections) throws Exception {
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                appName,
                queryData,
                false,
                EntityListResponse.class);

        assert entityListResponse.getEntities().length == 1;
        assert entityListResponse.getEntities()[0].getId().equals(PARENT_CASE_ID);
    }

    private void testParentSelection(String appName, QueryData queryData, ArrayList<String> selections) throws Exception {
        selections.add(PARENT_CASE_ID);
        CommandListResponseBean commandListResponseBean = sessionNavigateWithQuery(selections,
                appName,
                queryData,
                false,
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
                false,
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
                false,
                EntityListResponse.class);
        testChildSearchResult(appName, queryData, selections, subCaseSelectionId);
        testChildSelection(appName, queryData, selections, subCaseSelectionId);
    }

    // test result of executing child search
    private void testChildSearchResult(String appName, QueryData queryData, ArrayList<String> selections, String subCaseSelectionId) throws Exception {
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                appName,
                queryData,
                false,
                EntityListResponse.class);

        assert entityListResponse.getEntities().length == 1;
        assert entityListResponse.getEntities()[0].getId().equals(subCaseSelectionId);
    }

    // selecting child should show child update form
    private void testChildSelection(String appName, QueryData queryData, ArrayList<String> selections, String subCaseSelectionId) throws Exception {
        selections.add(subCaseSelectionId);
        CommandListResponseBean commandListResponseBean = sessionNavigateWithQuery(selections,
                appName,
                queryData,
                false,
                CommandListResponseBean.class);

        assert commandListResponseBean.getCommands().length == 1;
        assert commandListResponseBean.getSelections().length == 4;
        assert commandListResponseBean.getSelections()[3].equals(subCaseSelectionId);

        selections.add(INDEX_CHILD_FORM);
        sessionNavigateWithQuery(selections,
                appName,
                queryData,
                false,
                NewFormResponse.class);
    }


    private void testChildSeeMore(String appName, QueryData queryData,
                                  ArrayList<String> selections, String subCaseSelectionId) throws Exception {
        // we see a case list first
        selections.add(INDEX_PARENT_SEARCH_FIRST_CHILD_SEE_MORE);
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                APP_CASE_CLAIM_SPF_PARENT,
                queryData,
                false,
                EntityListResponse.class);

        // clicking search moves user to search results screen directly
        selections.add("action 0");

        testChildSearchResult(appName, queryData, selections, subCaseSelectionId);
        testChildSelection(appName, queryData, selections, subCaseSelectionId);
    }

    private void configureSyncMock() {
        when(syncRequester.makeSyncRequest(anyString(), anyString(), any(HttpHeaders.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
    }

    private void configureQueryMock() {
        when(queryRequester.makeQueryRequest(eq("https://staging.commcarehq.org/a/bosco/phone/search/?case_type=song"), any(HttpHeaders.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "query_responses/case_claim_parent_child_response.xml"));

        when(queryRequester.makeQueryRequest(eq("https://staging.commcarehq.org/a/bosco/phone/search/?case_type=show"), any(HttpHeaders.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "query_responses/case_claim_parent_child_child_response.xml"));
    }
}
