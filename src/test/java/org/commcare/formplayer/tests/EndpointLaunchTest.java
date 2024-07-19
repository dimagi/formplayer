package org.commcare.formplayer.tests;

import static org.commcare.formplayer.util.Constants.TOGGLE_SESSION_ENDPOINTS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.PeristentCommand;
import org.commcare.formplayer.beans.menus.CommandUtils.NavIconState;
import org.commcare.formplayer.mocks.FormPlayerPropertyManagerMock;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.services.BrowserValuesProvider;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.utils.WithHqUser;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.Property;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.util.NestedServletException;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Do launch tests for very basic session endpoint definitions
 */
@WebMvcTest
public class EndpointLaunchTest extends BaseTestClass {

    private final String APP_NAME = "endpoint";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("endpointdomain", "endpointusername");
        storageFactoryMock.configure("endpointusername", "endpointdomain", "app_id", "asUser");
        FormPlayerPropertyManagerMock.mockAutoAdvanceMenu(storageFactoryMock);
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/caseclaim.xml";
    }

    @Test
    @WithHqUser(enabledToggles = {})
    public void testToggleOff() throws Exception {
        assertThrows(NestedServletException.class, () -> {
            CommandListResponseBean commandListResponse = sessionNavigateWithEndpoint(APP_NAME,
                    "nope",
                    null,
                    CommandListResponseBean.class);
        });
    }

    @Test
    @WithHqUser(enabledToggles = {TOGGLE_SESSION_ENDPOINTS})
    public void testEndpoints() throws Exception {
        CommandListResponseBean commandListResponse = sessionNavigateWithEndpoint(APP_NAME,
                "caselist",
                null,
                CommandListResponseBean.class);
        assert commandListResponse.getCommands().length == 2;
        assert commandListResponse.getCommands()[0].getDisplayText().contentEquals("Add Parent");
        assert commandListResponse.getCommands()[1].getDisplayText().contentEquals("Followup");
        assertArrayEquals(commandListResponse.getSelections(), new String[]{"0"});


        NewFormResponse formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "add_parent",
                null,
                NewFormResponse.class);
        assert formResponse.getTitle().contentEquals("Add Parent");
        assertArrayEquals(formResponse.getSelections(), new String[]{"0", "0"});

        HashMap<String, String> endpointArgs = new HashMap<>();
        endpointArgs.put("case_id", "94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18");
        formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "followup",
                endpointArgs,
                NewFormResponse.class);
        assert formResponse.getTitle().contentEquals("Followup");
        assert formResponse.getBreadcrumbs()[3].contentEquals("Batman Begins");
        assertArrayEquals(formResponse.getSelections(),
                new String[]{"0", "1", "94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18"});

        commandListResponse = sessionNavigateWithEndpoint(APP_NAME,
                "parents",
                endpointArgs,
                CommandListResponseBean.class);
        assert commandListResponse.getCommands().length == 2;
        assert commandListResponse.getCommands()[0].getDisplayText().contentEquals("Add Child");
        assert commandListResponse.getCommands()[1].getDisplayText().contentEquals(
                "Child Case List");
        assertArrayEquals(commandListResponse.getSelections(),
                new String[]{"1", "94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18"});

        formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "add_child",
                endpointArgs,
                NewFormResponse.class);
        assert formResponse.getTitle().contentEquals("Add Child");
        assertArrayEquals(formResponse.getSelections(),
                new String[]{"1", "94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18", "0"});

        // Since auto-advance is true, the endpoint endpoint advances to open the form from the
        // case list
        endpointArgs.put("case_id_child_case", "f04bf0e8-2001-4885-a724-5497b34abe95");
        formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "child_case_list",
                endpointArgs,
                NewFormResponse.class);
        assert formResponse.getTitle().contentEquals("Update Child");
        assert formResponse.getBreadcrumbs()[4].contentEquals("The Dark Knight");
        assertArrayEquals(formResponse.getSelections(),
                new String[]{"1", "94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18", "1",
                        "f04bf0e8-2001-4885-a724-5497b34abe95"});

        formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "update_child",
                endpointArgs,
                NewFormResponse.class);
        assert formResponse.getTitle().contentEquals("Update Child");
        assert formResponse.getBreadcrumbs()[4].contentEquals("The Dark Knight");
        assertArrayEquals(formResponse.getSelections(),
                new String[]{"1", "94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18", "1",
                        "f04bf0e8-2001-4885-a724-5497b34abe95"});
    }

    @Test
    @WithHqUser(enabledToggles = {TOGGLE_SESSION_ENDPOINTS})
    public void testEndpointsRelevancy() throws Exception {
        // With respect-relevancy set to false, we can navigate to the hidden form
        HashMap<String, String> endpointArgs = new HashMap<>();
        endpointArgs.put("case_id", "94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18");
        NewFormResponse formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "add_child_not_respect_relevancy",
                endpointArgs,
                NewFormResponse.class);
        assert formResponse.getTitle().contentEquals("Add Child");

         formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "add_child_not_respect_relevancy_m0_f1",
                endpointArgs,
                NewFormResponse.class);
        assert formResponse.getTitle().contentEquals("Followup");

        // With respect-relevancy set, we can't navigate to the hidden form
        Assertions.assertThrows(Exception.class, () -> {
            sessionNavigateWithEndpoint(APP_NAME,
                    "add_child_respect_relevancy",
                    endpointArgs,
                    NewFormResponse.class);
        });
    }


    @Test
    @WithHqUser(enabledToggles = {TOGGLE_SESSION_ENDPOINTS})
    public void testPeristentMenuForEndpointLaunch() throws Exception {
        CommandListResponseBean commandListResponse = sessionNavigateWithEndpoint(APP_NAME,
                "caselist",
                null,
                CommandListResponseBean.class);
        ArrayList<PeristentCommand> expectedMenu = new ArrayList<>();
        expectedMenu.add(new PeristentCommand("0", "Case List", null, NavIconState.NEXT));
        expectedMenu.add(new PeristentCommand("1", "Parents", null, NavIconState.NEXT));
        PeristentCommand parentMenu = expectedMenu.get(0);
        parentMenu.addCommand(new PeristentCommand("0", "Add Parent", null, NavIconState.JUMP));
        parentMenu.addCommand(new PeristentCommand("1", "Followup", null, NavIconState.NEXT));
        assertEquals(expectedMenu, commandListResponse.getPersistentMenu());

        NewFormResponse formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "add_parent",
                null,
                NewFormResponse.class);
        assertEquals(expectedMenu, formResponse.getPersistentMenu());


        HashMap<String, String> endpointArgs = new HashMap<>();
        String caseSelection = "94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18";
        endpointArgs.put("case_id", caseSelection);
        formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "followup",
                endpointArgs,
                NewFormResponse.class);
        PeristentCommand followupMenu = parentMenu.getCommands().get(1);
        followupMenu.addCommand(new PeristentCommand(caseSelection, "Batman Begins"));
        assertEquals(expectedMenu, formResponse.getPersistentMenu());

        commandListResponse = sessionNavigateWithEndpoint(APP_NAME,
                "parents",
                endpointArgs,
                CommandListResponseBean.class);
        expectedMenu = new ArrayList<>();
        expectedMenu.add(new PeristentCommand("0", "Case List", null, NavIconState.NEXT));
        expectedMenu.add(new PeristentCommand("1", "Parents", null, NavIconState.NEXT));
        parentMenu = expectedMenu.get(1);
        parentMenu.addCommand(new PeristentCommand(caseSelection, "Batman Begins"));
        PeristentCommand batmanBeginsMenu = parentMenu.getCommands().get(0);
        batmanBeginsMenu.addCommand(new PeristentCommand("0", "Add Child", null, NavIconState.JUMP));
        batmanBeginsMenu.addCommand(new PeristentCommand("1", "Child Case List", null, NavIconState.NEXT));
        assertEquals(expectedMenu, commandListResponse.getPersistentMenu());

        formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "add_child",
                endpointArgs,
                NewFormResponse.class);
        assertEquals(expectedMenu, formResponse.getPersistentMenu());

        String childCaseSelection = "f04bf0e8-2001-4885-a724-5497b34abe95";
        endpointArgs.put("case_id_child_case", childCaseSelection);
        formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "child_case_list",
                endpointArgs,
                NewFormResponse.class);
        PeristentCommand childCaseListMenu = batmanBeginsMenu.getCommands().get(1);
        childCaseListMenu.addCommand(new PeristentCommand(childCaseSelection, "The Dark Knight"));
        PeristentCommand darkKnightMenu = childCaseListMenu.getCommands().get(0);
        darkKnightMenu.addCommand(new PeristentCommand("0", "Update Child", null, NavIconState.JUMP));
        assertEquals(expectedMenu, formResponse.getPersistentMenu());

        formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "update_child",
                endpointArgs,
                NewFormResponse.class);
        assertEquals(expectedMenu, formResponse.getPersistentMenu());
    }

    @Test
    @WithHqUser(enabledToggles = {TOGGLE_SESSION_ENDPOINTS})
    public void testPeristentMenuForEndpointLaunchWithoutRespectRelevancy() throws Exception {
        NewFormResponse formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "add_parent_not_respect_relevancy",
                null,
                NewFormResponse.class);
        // Verify that we only add root menu
        ArrayList<PeristentCommand> expectedMenu = new ArrayList<>();
        expectedMenu.add(new PeristentCommand("0", "Case List", null, NavIconState.NEXT));
        expectedMenu.add(new PeristentCommand("1", "Parents", null, NavIconState.NEXT));
        assertEquals(expectedMenu, formResponse.getPersistentMenu());
    }
}
