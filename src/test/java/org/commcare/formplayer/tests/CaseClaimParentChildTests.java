package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.QueryResponseBean;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CaseClaimParentChildTests extends BaseTestClass {

    private static final String PARENT_CASE_ID = "192262cb-fbfa-46a4-ba91-a9d13659b0e0";
    private static final String CHILD_CASE_ID = "d29c08f5-943c-42ca-b0a5-f64cbddba087";

    private final String INDEX_SONGS = "0";
    private final String INDEX_SONGS_SEARCH_FIRST = "1";
    private final String INDEX_SONGS_SEE_MORE = "2";
    private final String INDEX_SONGS_SKIP_TO_RESULTS = "3";

    private final String INDEX_SONGS_SEARCH_FIRST_SHOWS = "1";
    private final String INDEX_SONGS_SEARCH_FIRST_SHOWS_SEARCH_FIRST = "2";
    private final String INDEX_SONGS_SEARCH_FIRST_SHOWS_SEE_MORE = "3";
    private final String INDEX_SONGS_SEARCH_FIRST_SHOWS_SKIP_TO_RESULTS = "4";

    private final String INDEX_MODULE_UPDATE_SHOW = "0";


    @Override
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
    public void testParentSearchFirst() throws Exception {
        ArrayList<String> selections = new ArrayList<>();
        selections.add(INDEX_SONGS_SEARCH_FIRST);

        // we see a search screen first
        sessionNavigateWithQuery(selections,
                "case_claim_spf_parent",
                null,
                false,
                QueryResponseBean.class);

        // execute search
        QueryData queryData = new QueryData();
        queryData.setExecute("search_command.m5", true);

        testParentSearchResults(queryData, selections);
        testParentSelection(queryData, selections);

        testChildSearchNormal(queryData, new ArrayList<>(selections), "search_command.m6");
        testChildSearchFirst(queryData, new ArrayList<>(selections), "search_command.m7");
        testChildSeeMore(queryData, new ArrayList<>(selections));
        testChildSkipToResults(queryData, new ArrayList<>(selections));
    }

    private void testChildSearchNormal(QueryData queryData, ArrayList<String> selections, String searchKey) throws Exception {
        selections.add(INDEX_SONGS_SEARCH_FIRST_SHOWS);
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                "case_claim_spf_parent",
                queryData,
                false,
                EntityListResponse.class);

        // clicking search moves user to search screen
        selections.add("action 0");

        sessionNavigateWithQuery(selections,
                "case_claim_spf_parent",
                queryData,
                false,
                QueryResponseBean.class);
        queryData.setExecute(searchKey, true);

        testChildSearchResult(queryData, selections);
        testChildSelection(queryData, selections);
    }

    @Test
    public void testParentSeeMore() throws Exception {
        ArrayList<String> selections = new ArrayList<>();
        selections.add(INDEX_SONGS_SEE_MORE);
        QueryData queryData = new QueryData();

        // we should see parent case list with search action
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                "case_claim_spf_parent",
                queryData,
                false,
                EntityListResponse.class);

        assert entityListResponse.getActions().length == 1;

        // move to search screen
        selections.add("action 0");
        sessionNavigateWithQuery(selections,
                "case_claim_spf_parent",
                queryData,
                false,
                EntityListResponse.class);

        queryData.setExecute("search_command.m10", true);
        testParentSearchResults(queryData, selections);
        testParentSelection(queryData, selections);

        testChildSearchFirst(queryData, new ArrayList<>(selections), "search_command.m12");
        testChildSeeMore(queryData, new ArrayList<>(selections));
        testChildSkipToResults(queryData, new ArrayList<>(selections));
    }

    @Test
    public void testParentSkipToSearchResults() throws Exception {
        ArrayList<String> selections = new ArrayList<>();
        selections.add(INDEX_SONGS_SKIP_TO_RESULTS);

        // we should move directly to search results
        QueryData queryData = new QueryData();
        testParentSearchResults(queryData, selections);
        testParentSelection(queryData, selections);

        testChildSearchFirst(queryData, new ArrayList<>(selections), "search_command.m17");
        testChildSeeMore(queryData, new ArrayList<>(selections));
        testChildSkipToResults(queryData, new ArrayList<>(selections));
    }

    private void testParentSearchResults(QueryData queryData, ArrayList<String> selections) throws Exception {
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                "case_claim_spf_parent",
                queryData,
                false,
                EntityListResponse.class);

        assert entityListResponse.getEntities().length == 1;
        assert entityListResponse.getEntities()[0].getId().equals(PARENT_CASE_ID);
    }

    private void testParentSelection(QueryData queryData, ArrayList<String> selections) throws Exception {
        selections.add(PARENT_CASE_ID);
        CommandListResponseBean commandListResponseBean = sessionNavigateWithQuery(selections,
                "case_claim_spf_parent",
                queryData,
                false,
                CommandListResponseBean.class);

        assert commandListResponseBean.getCommands().length == 5;
    }

    private void testChildSearchFirst(QueryData queryData, ArrayList<String> selections, String searchKey) throws Exception {
        // we move to child search directly
        selections.add(INDEX_SONGS_SEARCH_FIRST_SHOWS_SEARCH_FIRST);
        sessionNavigateWithQuery(selections,
                "case_claim_spf_parent",
                queryData,
                false,
                QueryResponseBean.class);

        // Execute child search
        queryData.setExecute(searchKey, true);

        testChildSearchResult(queryData, selections);
        testChildSelection(queryData, selections);
    }

    private void testChildSkipToResults(QueryData queryData, ArrayList<String> selections) throws Exception {
        // we move to child search results directly
        selections.add(INDEX_SONGS_SEARCH_FIRST_SHOWS_SKIP_TO_RESULTS);
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                "case_claim_spf_parent",
                queryData,
                false,
                EntityListResponse.class);
        testChildSearchResult(queryData, selections);
        testChildSelection(queryData, selections);
    }

    // test result of executing child search
    private void testChildSearchResult(QueryData queryData, ArrayList<String> selections) throws Exception {
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                "case_claim_spf_parent",
                queryData,
                false,
                EntityListResponse.class);

        assert entityListResponse.getEntities().length == 1;
        assert entityListResponse.getEntities()[0].getId().equals(CHILD_CASE_ID);
    }

    // selecting child should show child update form
    private void testChildSelection(QueryData queryData, ArrayList<String> selections) throws Exception {
        selections.add(CHILD_CASE_ID);
        CommandListResponseBean commandListResponseBean = sessionNavigateWithQuery(selections,
                "case_claim_spf_parent",
                queryData,
                false,
                CommandListResponseBean.class);

        assert commandListResponseBean.getCommands().length == 1;
        assert commandListResponseBean.getSelections().length == 4;
        assert commandListResponseBean.getSelections()[3].equals(CHILD_CASE_ID);

        selections.add(INDEX_MODULE_UPDATE_SHOW);
        sessionNavigateWithQuery(selections,
                "case_claim_spf_parent",
                queryData,
                false,
                NewFormResponse.class);
    }


    private void testChildSeeMore(QueryData queryData, ArrayList<String> selections) throws Exception {
        // we see a case list first
        selections.add(INDEX_SONGS_SEARCH_FIRST_SHOWS_SEE_MORE);
        EntityListResponse entityListResponse = sessionNavigateWithQuery(selections,
                "case_claim_spf_parent",
                queryData,
                false,
                EntityListResponse.class);

        // clicking search moves user to search results screen directly
        selections.add("action 0");

        testChildSearchResult(queryData, selections);
        testChildSelection(queryData, selections);
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
