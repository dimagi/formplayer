package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashMap;

/**
 * Do launch tests for very basic session endpoint definitions
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class EndpointLaunchTest extends BaseTestClass {

    private final String APP_NAME = "endpoint";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("endpointdomain", "endpointusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/caseclaim.xml";
    }

    @Test
    public void testEndpoints() throws Exception {

        CommandListResponseBean commandListResponse = sessionNavigateWithEndpoint(APP_NAME,
                "caselist",
                null,
                CommandListResponseBean.class);
        assert commandListResponse.getCommands().length == 2;
        assert commandListResponse.getCommands()[0].getDisplayText().contentEquals("Add Parent");
        assert commandListResponse.getCommands()[1].getDisplayText().contentEquals("Followup");


        NewFormResponse formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "add_parent",
                null,
                NewFormResponse.class);
        assert formResponse.getTitle().contentEquals("Add Parent");

        HashMap<String, String> endpointArgs = new HashMap<>();
        endpointArgs.put("case_id","94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18");
        formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "followup",
                endpointArgs,
                NewFormResponse.class);
        assert formResponse.getTitle().contentEquals("Followup");
        assert formResponse.getBreadcrumbs()[3].contentEquals("Batman Begins");

        commandListResponse = sessionNavigateWithEndpoint(APP_NAME,
                "parents",
                endpointArgs,
                CommandListResponseBean.class);
        assert commandListResponse.getCommands().length == 2;
        assert commandListResponse.getCommands()[0].getDisplayText().contentEquals("Add Child");
        assert commandListResponse.getCommands()[1].getDisplayText().contentEquals("Child Case List");

        formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "add_child",
                endpointArgs,
                NewFormResponse.class);
        assert formResponse.getTitle().contentEquals("Add Child");

        // Since auto-advance is true, the endpoint endpoint advances to open the form from the case list
        endpointArgs.put("case_id_child_case","f04bf0e8-2001-4885-a724-5497b34abe95");
        formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "child_case_list",
                endpointArgs,
                NewFormResponse.class);
        assert formResponse.getTitle().contentEquals("Update Child");
        assert formResponse.getBreadcrumbs()[4].contentEquals("The Dark Knight");

        formResponse = sessionNavigateWithEndpoint(APP_NAME,
                "update_child",
                endpointArgs,
                NewFormResponse.class);
        assert formResponse.getTitle().contentEquals("Update Child");
    }
}
