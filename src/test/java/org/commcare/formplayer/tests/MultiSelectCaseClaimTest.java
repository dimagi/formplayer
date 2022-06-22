package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Multimap;

import org.commcare.formplayer.beans.FormEntryResponseBean;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.MockRequestUtils;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.util.screen.MultiSelectEntityScreen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for selecting and claiming multiple entities from case search results screen
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class MultiSelectCaseClaimTest extends BaseTestClass {

    private static final String APP_NAME = "case_claim_with_multi_select";
    @Autowired
    CacheManager cacheManager;

    @Captor
    ArgumentCaptor<String> urlCaptor;

    @Captor
    ArgumentCaptor<MultiValueMap<String, String>> requestDataCaptor;
    private MockRequestUtils mockRequest;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("caseclaimdomain", "caseclaimusername");
        cacheManager.getCache("case_search").clear();
        mockRequest = new MockRequestUtils(webClientMock, restoreFactoryMock);
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/caseclaim.xml";
    }

    @Test
    public void testCaseClaimWithMultiSelectList() throws Exception {
        // default search is on so we should skip to search results directly
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_search_multi_select_response.xml")) {
            EntityListResponse entityResp = sessionNavigateWithQuery(new String[]{"0", "action 0"},
                    APP_NAME,
                    null,
                    EntityListResponse.class);
            assertTrue(entityResp.isMultiSelect());
        }

        String[] selectedValues =
                new String[]{"94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18", "0156fa3e-093e-4136-b95c-01b13dae66c7",
                        "0156fa3e-093e-4136-b95c-01b13dae66c8"};
        String[] selections = new String[]{"0", "action 0", MultiSelectEntityScreen.USE_SELECTED_VALUES};
        CommandListResponseBean commandResponse = sessionNavigateWithQuery(selections,
                APP_NAME,
                null,
                selectedValues,
                CommandListResponseBean.class);

        // `use_selected_values' should be replaced in returned selections
        Assertions.assertNotEquals(selections, commandResponse.getSelections());

        // Verify case claim request
        verify(webClientMock, times(1)).caseClaimPost(urlCaptor.capture(), requestDataCaptor.capture());
        assertEquals("http://localhost:8000/a/test/phone/claim-case/", urlCaptor.getAllValues().get(0));
        MultiValueMap<String, String> requestData = requestDataCaptor.getAllValues().get(0);

        // cases that are owned should not be in the claim request
        List<String> casesToBeClaimed = Arrays.asList("0156fa3e-093e-4136-b95c-01b13dae66c7",
                "0156fa3e-093e-4136-b95c-01b13dae66c8");
        assertEquals(requestData.get("case_id"), casesToBeClaimed);
    }

    @Test
    public void testNoCaseClaimRequestWhenAllCasesOwned() throws Exception {
        String[] selectedValues = new String[]{"94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18"};
        String[] selections = new String[]{"0", "action 0", MultiSelectEntityScreen.USE_SELECTED_VALUES};
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_search_multi_select_response.xml")) {
            sessionNavigateWithQuery(selections,
                    APP_NAME,
                    null,
                    selectedValues,
                    CommandListResponseBean.class);
        }
        // Verify case claim request should not fire if the selected cases were owned already
        verify(webClientMock, times(0)).post(any(), any());
    }

    @Test
    public void testAutoLaunchAction() throws Exception {
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_search_multi_select_response.xml")) {
            EntityListResponse entityResp = sessionNavigateWithQuery(new String[]{"1"},
                    APP_NAME,
                    null,
                    EntityListResponse.class);
            assertTrue(entityResp.isMultiSelect());
        }

        String[] selectedValues =
                new String[]{"94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18", "0156fa3e-093e-4136-b95c-01b13dae66c7",
                        "0156fa3e-093e-4136-b95c-01b13dae66c8"};
        String[] selections = new String[]{"1", MultiSelectEntityScreen.USE_SELECTED_VALUES};
        CommandListResponseBean commandResponse;
        try (
                MockRequestUtils.VerifiedMock ignoredPostMock = mockRequest.mockPost(true);
                MockRequestUtils.VerifiedMock ignoredRestoreMock = mockRequest.mockRestore("restores/multi_select_case_claim.xml");
        ) {
            commandResponse = sessionNavigateWithQuery(selections,
                    APP_NAME,
                    null,
                    selectedValues,
                    CommandListResponseBean.class);
        }
        assertEquals("Close", commandResponse.getCommands()[0].getDisplayText());

        // Clear the cache to ensure that we aren't using it.
        cacheManager.getCache("case_search").clear();

        ArrayList<String> updatedSelections = new ArrayList<>();
        updatedSelections.addAll(Arrays.asList(commandResponse.getSelections()));
        updatedSelections.add("0");

        sessionNavigateWithQuery(updatedSelections.toArray(new String[0]),
                APP_NAME,
                null,
                FormEntryResponseBean.class);

        // Verify query request should not happen again
        verify(webClientMock, times(0)).postFormData(any(), any());
    }
}
