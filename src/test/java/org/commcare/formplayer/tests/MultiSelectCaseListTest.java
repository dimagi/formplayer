package org.commcare.formplayer.tests;

import static org.commcare.formplayer.junit.HasXpath.hasXpath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.commcare.formplayer.beans.EvaluateXPathResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.junit.HasXpath;
import org.commcare.formplayer.junit.RestoreFactoryAnswer;
import org.commcare.formplayer.junit.request.EvaluateXpathRequest;
import org.commcare.formplayer.mocks.FormPlayerPropertyManagerMock;
import org.commcare.formplayer.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.util.Assert;

import java.util.HashMap;

/**
 * Tests Navigation involving a multi-select case list
 */
@WebMvcTest
public class MultiSelectCaseListTest extends BaseTestClass {

    private static final String APP = "multi_select_case_list";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("caseclaimdomain", "caseclaimusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/caseclaim.xml";
    }

    @Test
    public void testNormalMultiSelectCaseList() throws Exception {
        String[] selections = new String[]{"0", "1"};
        EntityListResponse entityListResp = sessionNavigate(selections, APP,
                EntityListResponse.class);
        Assert.isTrue(entityListResp.isMultiSelect(),
                "Multi Select should be turned on for instance-datum backed entity list");
        Assert.isTrue(entityListResp.getMaxSelectValue() == 10, "max-select-value is not set correctly");

        selections = new String[]{"0", "1", "use_selected_values"};
        String[] selectedValues =
                new String[]{"5e421eb8bf414e03b4871195b869d894", "3512eb7c-7a58-4a95-beda-205eb0d7f163"};
        NewFormResponse formResp = sessionNavigateWithSelectedValues(selections, APP, selectedValues,
                NewFormResponse.class);

        // use_selected_values would be replaced by guid in the selections array in response
        assertFalse(formResp.getSelections()[2].contentEquals("use_selected_values"));

        // Navigate without using selected values using the selections from response
        selections = formResp.getSelections();
        NewFormResponse formRespUsingGuid = sessionNavigate(selections, APP, NewFormResponse.class);
        assertArrayEquals(formResp.getBreadcrumbs(), formRespUsingGuid.getBreadcrumbs());
        checkForSelectedEntitiesDatum(formRespUsingGuid.getSessionId(), selections);
        checkForSelectedEntitiesInstance(formRespUsingGuid.getSessionId(), selectedValues);
    }

    @Test
    public void testConfirmedSelectionsForMultiSelectCaseList() {
        String[] selections = new String[]{"0", "1", "use_selected_values"};
        String[] selectedValues =
                new String[]{"5e421eb8bf414e03b4871195b869d894", "3512eb7c-7a58-4a95-beda-205eb0d7f163"};
        try {
            NewFormResponse newFormResponse = sessionNavigateWithSelectedValues(selections, APP, selectedValues,
                    NewFormResponse.class);
            assertFalse(restoreFactoryMock.isConfirmedSelection(selections));
            assertTrue(restoreFactoryMock.isConfirmedSelection(newFormResponse.getSelections()));
        } catch (Exception e) {
            fail("Session Navigation failed for pre-validated input", e);
        }
    }


    // Ensure that 'selected_cases' instance is populated correctly
    private void checkForSelectedEntitiesInstance(String sessionId, String[] expectedCases) throws Exception {
        EvaluateXPathResponseBean evaluateXpathResponseBean = evaluateXPath(sessionId,
                "instance('selected_cases')/results");
        assertEquals(evaluateXpathResponseBean.getStatus(), Constants.ANSWER_RESPONSE_STATUS_POSITIVE);
        for (int i = 0; i < expectedCases.length; i++) {
            String xpathRef = "/result/results[@id='selected_cases']/value[" + (i+1) + "]";
            assertThat(evaluateXpathResponseBean.getOutput(), hasXpath(xpathRef, equalTo(expectedCases[i])));
        }

    }

    // Ensure that the instance datum is set correctly to the guid
    private void checkForSelectedEntitiesDatum(String sessionId, String[] selections) throws Exception {
        EvaluateXPathResponseBean evaluateXpathResponseBean = evaluateXPath(sessionId,
                "instance('commcaresession')/session/data/selected_cases");
        assertEquals(evaluateXpathResponseBean.getStatus(), Constants.ANSWER_RESPONSE_STATUS_POSITIVE);
        String guid = selections[selections.length - 1];
        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<result>" + guid + "</result>\n";
        assertEquals(evaluateXpathResponseBean.getOutput(), result);
    }

    @Test
    public void testAutoAdvanceWithMultiSelect() throws Exception {
        FormPlayerPropertyManagerMock.mockAutoAdvanceMenu(storageFactoryMock);
        String[] selections = new String[]{"1"};
        sessionNavigate(selections, APP,
                EntityListResponse.class);

        selections = new String[]{"1", "use_selected_values"};
        String[] selectedValues =
                new String[]{"5e421eb8bf414e03b4871195b869d894", "3512eb7c-7a58-4a95-beda-205eb0d7f163"};
        NewFormResponse formResp = sessionNavigateWithSelectedValues(selections, APP, selectedValues,
                NewFormResponse.class);

        // selections should now be {"1", "guid"} without the auto-advanced menu index
        assertEquals(2, formResp.getSelections().length);
    }

    @Test
    public void testEofNavigationRetainsSelectedCases() throws Exception {
        String[] selections = new String[]{"0", "1", "use_selected_values"};
        String[] selectedValues =
                new String[]{"5e421eb8bf414e03b4871195b869d894", "3512eb7c-7a58-4a95-beda-205eb0d7f163"};
        NewFormResponse formResp = sessionNavigateWithSelectedValues(selections, APP, selectedValues,
                NewFormResponse.class);
        SubmitResponseBean submitResponse = submitForm(
                new HashMap<>(),
                formResp.getSessionId()
        );
        assertTrue(submitResponse.getStatus().contentEquals("success"));
        NewFormResponse newFormResponse = getNextScreenForEofNavigation(submitResponse,
                NewFormResponse.class);
        checkForSelectedEntitiesDatum(newFormResponse.getSessionId(), newFormResponse.getSelections());
        checkForSelectedEntitiesInstance(newFormResponse.getSessionId(), selectedValues);
    }

    @Test
    public void testAutoSelectionWithMultiSelectCaseList() throws Exception {
        String[] selections = new String[]{"0", "2"};
        NewFormResponse formResp = sessionNavigate(selections, APP,
                NewFormResponse.class);
        // we do not add any guid to selections in case of auto-selection
        assertArrayEquals(formResp.getSelections(), selections);
        String[] allCases = new String[]{
                "56306779-26a2-4aa5-a952-70c9d8b21e39", "5e421eb8bf414e03b4871195b869d894",
                "3512eb7c-7a58-4a95-beda-205eb0d7f163", "94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18",
                "f70977c4b27f44d391e118592ef8d08b", "b503dc77-f240-4d1e-89cd-69958f52bec4",
                "3a028cab-fa70-4611-a423-046d25f3e2f4"
        };
        checkForSelectedEntitiesInstance(formResp.getSessionId(), allCases);

        // we don't know the generated guid in this case to match against,
        // but confirm there is a value saved for the corresponding instance datum in the session
        EvaluateXPathResponseBean evaluateXpathResponseBean = evaluateXPath(formResp.getSessionId(),
                "instance('commcaresession')/session/data/selected_cases");
        assertEquals(evaluateXpathResponseBean.getStatus(), Constants.ANSWER_RESPONSE_STATUS_POSITIVE);

        String result = evaluateXpathResponseBean.getOutput();
        result = result.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<result>", "");
        result = result.replace("</result>\n", "");
        assertTrue(result.length() == 36);
    }

    @Test
    public void testAutoSelectionWithMultiSelectCaseList_MaxCasesError() {
        String[] selections = new String[]{"0", "3"};
        try {
            sessionNavigate(selections, APP, NewFormResponse.class);
        } catch (Exception e) {
            assertEquals("Too many cases(7) to proceed. Only 5 are allowed",
                    e.getCause().getMessage());
        }
    }

    @Test
    public void testAutoSelectionWithMultiSelectCaseList_NoCaseSelection() throws Exception {
        RestoreFactoryAnswer answer = new RestoreFactoryAnswer("restores/nocases.xml");
        doAnswer(answer).when(restoreFactoryMock).getRestoreXml(anyBoolean());

        // Since there are no cases to select we see the empty case list
        String[] selections = new String[]{"0", "2"};
        EntityListResponse response = sessionNavigate(selections, APP, EntityListResponse.class);
        assertEquals(response.getEntities().length, 0);

        // we can still select any actions present on the case list
        selections = new String[]{"0", "2","action 0"};
        sessionNavigate(selections, APP, NewFormResponse.class);
    }
}
