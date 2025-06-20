package org.commcare.formplayer.tests;

import static org.commcare.formplayer.util.Constants.TOGGLE_SESSION_ENDPOINTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import org.apache.commons.lang3.ArrayUtils;
import org.commcare.formplayer.beans.FormEntryResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.PersistentCommand;
import org.commcare.formplayer.mocks.FormPlayerPropertyManagerMock;
import org.commcare.formplayer.utils.MockRequestUtils;
import org.commcare.formplayer.utils.WithHqUser;
import org.commcare.util.screen.MultiSelectEntityScreen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Tests for selecting and claiming multiple entities from case search results screen
 */
@WebMvcTest
public class MultiSelectCaseClaimTest extends BaseTestClass {

    private static final String APP_NAME = "case_claim_with_multi_select";
    @Autowired
    CacheManager cacheManager;

    @Captor
    ArgumentCaptor<String> urlCaptor;

    @Captor
    ArgumentCaptor<MultiValueMap<String, String>> requestDataCaptor;

    @Captor
    ArgumentCaptor<Multimap<String, String>> queryRequestDataCaptor;

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

        CommandListResponseBean commandResponse;

        try (
                MockRequestUtils.VerifiedMock ignoredPostMock = mockRequest.mockPost(true);
                MockRequestUtils.VerifiedMock ignoredRestoreMock = mockRequest.mockRestore("restores/caseclaim2.xml");
        ) {
            commandResponse = sessionNavigateWithQuery(selections,
                    APP_NAME,
                    null,
                    selectedValues,
                    CommandListResponseBean.class);
        }

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

        // Open a form and check the selected_values instance is correctly loaded
        String guid = commandResponse.getSelections()[1];
        selections = new String[]{"0", guid, "0"};
        NewFormResponse newFormResponse = sessionNavigateWithQuery(selections,
                APP_NAME,
                null,
                null,
                NewFormResponse.class);
        checkForSelectedEntitiesDatum(newFormResponse.getSessionId(), guid);
        checkForSelectedEntitiesInstance(newFormResponse.getSessionId(), selectedValues);
    }


    @Test
    public void testCaseClaimWithMultiSelectList_inline() throws Exception {
        // default search is on so we should skip to search results directly
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_search_multi_select_response.xml", 2)) {
            EntityListResponse entityResp = sessionNavigateWithQuery(new String[]{"4", "action 0"},
                    APP_NAME,
                    null,
                    EntityListResponse.class);
            assertTrue(entityResp.isMultiSelect());
            System.out.println("entityResp breadcrumbs: " + Arrays.toString(entityResp.getBreadcrumbs()));

        }
        String[] selectedValues =
                new String[]{"94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18", "0156fa3e-093e-4136-b95c-01b13dae66c7",
                        "0156fa3e-093e-4136-b95c-01b13dae66c8"};
        String[] selections = new String[]{"4", MultiSelectEntityScreen.USE_SELECTED_VALUES};

        CommandListResponseBean commandResponse;
        commandResponse = sessionNavigateWithQuery(selections,
                APP_NAME,
                null,
                selectedValues,
                CommandListResponseBean.class);
        System.out.println("commandResponse breadcrumbs: " + Arrays.toString(commandResponse.getBreadcrumbs()));
        // try (
        //         MockRequestUtils.VerifiedMock ignoredPostMock = mockRequest.mockPost(false);
        // ) {
        //     commandResponse = sessionNavigateWithQuery(selections,
        //             APP_NAME,
        //             null,
        //             selectedValues,
        //             CommandListResponseBean.class);
        // }

        // // `use_selected_values' should be replaced in returned selections
        // Assertions.assertNotEquals(selections, commandResponse.getSelections());


        // // Verify case claim request
        // verify(webClientMock, times(1)).caseClaimPost(urlCaptor.capture(), requestDataCaptor.capture());
        // assertEquals("http://localhost:8000/a/test/phone/claim-case/", urlCaptor.getAllValues().get(0));
        // MultiValueMap<String, String> requestData = requestDataCaptor.getAllValues().get(0);

        // // cases that are owned should not be in the claim request
        // List<String> casesToBeClaimed = Arrays.asList("0156fa3e-093e-4136-b95c-01b13dae66c7",
        //         "0156fa3e-093e-4136-b95c-01b13dae66c8");
        // assertEquals(requestData.get("case_id"), casesToBeClaimed);

        // // Open a form and check the selected_values instance is correctly loaded
        // String guid = commandResponse.getSelections()[1];
        // selections = new String[]{"0", guid, "0"};
        // NewFormResponse newFormResponse = sessionNavigateWithQuery(selections,
        //         APP_NAME,
        //         null,
        //         null,
        //         NewFormResponse.class);
        // checkForSelectedEntitiesDatum(newFormResponse.getSessionId(), guid);
        // checkForSelectedEntitiesInstance(newFormResponse.getSessionId(), selectedValues);
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
                MockRequestUtils.VerifiedMock ignoredRestoreMock = mockRequest.mockRestore(
                        "restores/multi_select_case_claim.xml");
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

    @Test
    public void testAutoSelection() throws Exception {
        CommandListResponseBean reponse;
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_search_multi_select_response.xml")) {
            reponse = sessionNavigateWithQuery(new String[]{"2"},
                    APP_NAME,
                    null,
                    CommandListResponseBean.class);

            // For auto-selection we should not add guid back to the selections.
            assertEquals(reponse.getSelections().length, 1);
            assertEquals(reponse.getSelections()[0], "2");
            assertEquals("Close", reponse.getCommands()[0].getDisplayText());

            // Persistent Menu And Breadcrumbs should not contain the auto-selected entities
            ArrayList<PersistentCommand> subMenu = reponse.getPersistentMenu().get(2).getCommands();
            assertEquals(1, subMenu.size());
            assertEquals("Close", subMenu.get(0).getDisplayText()); // directly contains the form instead of entity selection
            assertEquals(ImmutableList.of("Case Claim", "Follow Up"),
                    Arrays.stream(reponse.getBreadcrumbs()).toList());
        }

        ArrayList<String> updatedSelections = new ArrayList<>(Arrays.asList(reponse.getSelections()));
        updatedSelections.add("0");

        NewFormResponse formResponse = sessionNavigateWithQuery(updatedSelections.toArray(new String[0]),
                APP_NAME,
                null,
                NewFormResponse.class);
        ArrayList<PersistentCommand> subMenu = formResponse.getPersistentMenu().get(2).getCommands();
        assertEquals(1, subMenu.size());
        assertEquals("Close", subMenu.get(0).getDisplayText());
        assertEquals(ImmutableList.of("Case Claim", "Follow Up", "Close"),
                Arrays.stream(formResponse.getBreadcrumbs()).toList());
        String[] selectedValues = new String[]{"94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18",
                "0156fa3e-093e-4136-b95c-01b13dae66c7",
                "0156fa3e-093e-4136-b95c-01b13dae66c8"};
        checkForSelectedEntitiesInstance(formResponse.getSessionId(), selectedValues);
    }

    @Test
    public void testAutoSelection_WithNoCases() throws Exception {
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_search_no_cases_response.xml")) {
            // we see the entity list when there are no cases to select in auto select mode
            EntityListResponse entityListResponse = sessionNavigateWithQuery(new String[]{"2"},
                    APP_NAME,
                    null,
                    EntityListResponse.class);
            assertEquals(entityListResponse.getEntities().length, 0);

            // we can still select any actions present on the case list
            sessionNavigateWithQuery(new String[]{"2", "action 0"},
                    APP_NAME,
                    null,
                    NewFormResponse.class);
        }
    }

    @Test
    public void testAutoAdvanceMenuWithCaseSearch() throws Exception {
        FormPlayerPropertyManagerMock.mockAutoAdvanceMenu(storageFactoryMock, true);
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_search_multi_select_response.xml")) {
            EntityListResponse entityResp = sessionNavigateWithQuery(new String[]{"1"},
                    APP_NAME,
                    null,
                    EntityListResponse.class);
            assertTrue(entityResp.isMultiSelect());
        }

        String[] selectedValues =
                new String[]{"94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18", "0156fa3e-093e-4136-b95c-01b13dae66c7"};
        String[] selections = new String[]{"1", MultiSelectEntityScreen.USE_SELECTED_VALUES};
        CommandListResponseBean commandResponse;
        try (
                MockRequestUtils.VerifiedMock ignoredPostMock = mockRequest.mockPost(true);
                MockRequestUtils.VerifiedMock ignoredRestoreMock = mockRequest.mockRestore(
                        "restores/multi_select_case_claim.xml");
        ) {
            NewFormResponse formResponse = sessionNavigateWithQuery(selections,
                    APP_NAME,
                    null,
                    selectedValues,
                    NewFormResponse.class);

            // selections should now be {"1", "guid"} without the auto-advanced menu index
            assertEquals(formResponse.getSelections().length, 2);
        }
    }

    @Test
    @WithHqUser(enabledToggles = {TOGGLE_SESSION_ENDPOINTS})
    public void testMultiSelectEndpoint_ValidSelection() throws Exception {
        String[] selectedValues = new String[]{"94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18",
                "0156fa3e-093e-4136-b95c-01b13dae66c7",
                "0156fa3e-093e-4136-b95c-01b13dae66c8"};
        String selectedValuesArg = String.join(",", selectedValues);
        HashMap<String, String> endpointArgs = new HashMap<>();
        endpointArgs.put("selected_cases", selectedValuesArg);
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_search_multi_select_response.xml", 2)) {
            CommandListResponseBean commandResponse = sessionNavigateWithEndpoint(APP_NAME,
                    "case_list",
                    endpointArgs,
                    CommandListResponseBean.class);
            String[] formSelectionWithInstanceId = ArrayUtils.addAll(commandResponse.getSelections(), "0");
            NewFormResponse formResponse = sessionNavigateWithQuery(formSelectionWithInstanceId,
                    APP_NAME,
                    null,
                    NewFormResponse.class);
            checkForSelectedEntitiesInstance(formResponse.getSessionId(), selectedValues);
        }
    }

    @Test
    @WithHqUser(enabledToggles = {TOGGLE_SESSION_ENDPOINTS})
    public void testMultiSelectEndpoint_InvalidSelection() throws Exception {
        String[] selectedValues = new String[]{"94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18",
                "invalid_case_id",
                "0156fa3e-093e-4136-b95c-01b13dae66c8"};
        String selectedValuesArg = String.join(",", selectedValues);
        HashMap<String, String> endpointArgs = new HashMap<>();
        endpointArgs.put("selected_cases", selectedValuesArg);
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_search_multi_select_response.xml")) {
            Exception thrown = assertThrows(Exception.class, () ->
                    sessionNavigateWithEndpoint(APP_NAME,
                    "case_list",
                    endpointArgs,
                    CommandListResponseBean.class)
            );
            assertTrue(thrown.getMessage().contains("Could not select case invalid_case_id"));
        }
    }

    @Test
    @WithHqUser(enabledToggles = {TOGGLE_SESSION_ENDPOINTS})
    public void testMultiSelectEndpointWithClaim_ValidSelection() throws Exception {
        String[] selectedValues = new String[]{"94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18",
                "0156fa3e-093e-4136-b95c-01b13dae66c7",
                "0156fa3e-093e-4136-b95c-01b13dae66c8"};
        String selectedValuesArg = String.join(",", selectedValues);
        HashMap<String, String> endpointArgs = new HashMap<>();
        endpointArgs.put("selected_cases", selectedValuesArg);
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_search_multi_select_response.xml", 2)) {
            CommandListResponseBean commandResponse = sessionNavigateWithEndpoint(APP_NAME,
                    "case_list_with_claim",
                    endpointArgs,
                    CommandListResponseBean.class);
            String[] formSelectionWithInstanceId = ArrayUtils.addAll(commandResponse.getSelections(), "0");
            NewFormResponse formResponse = sessionNavigateWithQuery(formSelectionWithInstanceId,
                    APP_NAME,
                    null,
                    NewFormResponse.class);
            checkForSelectedEntitiesInstance(formResponse.getSessionId(), selectedValues);
        }
    }

    @Test
    @WithHqUser(enabledToggles = {TOGGLE_SESSION_ENDPOINTS})
    public void testCaseListEndpointWithInlineCaseSearch() throws Exception {
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_search_multi_select_response.xml")) {
            EntityListResponse response = sessionNavigateWithEndpoint(APP_NAME,
                    "inline_case_search_list_without_selection",
                    null,
                    EntityListResponse.class);
            assertNotNull(response.getEntities());
        }
    }


    @Test
    @WithHqUser(enabledToggles = {TOGGLE_SESSION_ENDPOINTS})
    public void testCaseListSelectionEndpointWithInlineCaseSearch() throws Exception {
        String[] selectedValues = new String[]{"94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18",
                "0156fa3e-093e-4136-b95c-01b13dae66c7",
                "0156fa3e-093e-4136-b95c-01b13dae66c8"};
        String selectedValuesArg = String.join(",", selectedValues);
        HashMap<String, String> endpointArgs = new HashMap<>();
        endpointArgs.put("selected_cases", selectedValuesArg);
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_search_multi_select_response.xml", 2)) {
            CommandListResponseBean response = sessionNavigateWithEndpoint(APP_NAME,
                    "inline_case_search_list",
                    endpointArgs,
                    CommandListResponseBean.class);
            assertEquals(response.getCommands().length, 1);
        }
        verify(webClientMock, times(2)).postFormData(urlCaptor.capture(),
                queryRequestDataCaptor.capture());

        assertEquals("http://localhost:8000/a/shubham/phone/case_fixture/d54c955d883b4dd99f57571649271af1/", urlCaptor.getAllValues().get(0));
        Multimap<String, String> requestData = queryRequestDataCaptor.getAllValues().get(0);
        assertEquals(String.join(",",requestData.get("case_id")), String.join(",",selectedValues));
    }

    @Test
    @WithHqUser(enabledToggles = {TOGGLE_SESSION_ENDPOINTS})
    public void testFormEndpointWithInlineCaseSearch() throws Exception {
        String[] selectedValues = new String[]{"94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18",
                "0156fa3e-093e-4136-b95c-01b13dae66c7",
                "0156fa3e-093e-4136-b95c-01b13dae66c8"};
        String selectedValuesArg = String.join(",", selectedValues);
        HashMap<String, String> endpointArgs = new HashMap<>();
        endpointArgs.put("selected_cases", selectedValuesArg);
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_search_multi_select_response.xml", 2)) {
            NewFormResponse formResponse = sessionNavigateWithEndpoint(APP_NAME,
                    "inline_case_search_form",
                    endpointArgs,
                    NewFormResponse.class);
            checkForSelectedEntitiesInstance(formResponse.getSessionId(), selectedValues);
        }
    }
}
