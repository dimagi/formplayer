package org.commcare.formplayer.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.commcare.formplayer.beans.EvaluateXPathResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
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
        assertThat(selections).isNotEqualTo(formResp.getSelections());

        // Navigate without using selected values using the selections from response
        selections = formResp.getSelections();
        NewFormResponse formRespUsingGuid = sessionNavigate(selections, APP, NewFormResponse.class);
        assertArrayEquals(formResp.getBreadcrumbs(), formRespUsingGuid.getBreadcrumbs());
        checkForSelectedEntitiesInstance(formRespUsingGuid.getSessionId(), selections);
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

    private void checkForSelectedEntitiesInstance(String sessionId, String[] selections) throws Exception {
        // Ensure that the datum is set correctly to the guid
        EvaluateXPathResponseBean evaluateXpathResponseBean = evaluateXPath(sessionId,
                "instance('commcaresession')/session/data/selected_cases");
        assertEquals(evaluateXpathResponseBean.getStatus(), Constants.ANSWER_RESPONSE_STATUS_POSITIVE);
        String guid = selections[selections.length - 1];
        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<result>" + guid + "</result>\n";
        assertEquals(evaluateXpathResponseBean.getOutput(), result);

        // Ensure that 'selected_cases' instance is populated correctly
        evaluateXpathResponseBean = evaluateXPath(sessionId,
                "instance('selected_cases')/results");
        assertEquals(evaluateXpathResponseBean.getStatus(), Constants.ANSWER_RESPONSE_STATUS_POSITIVE);
        result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<result>\n"
                + "  <results id=\"selected_cases\">\n"
                + "    <value>5e421eb8bf414e03b4871195b869d894</value>\n"
                + "    <value>3512eb7c-7a58-4a95-beda-205eb0d7f163</value>\n"
                + "  </results>\n"
                + "</result>\n";
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

        // selections should now be {"1", "guid"}
        assertEquals( 2, formResp.getSelections().length);
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
        checkForSelectedEntitiesInstance(newFormResponse.getSessionId(), newFormResponse.getSelections());
    }
}
