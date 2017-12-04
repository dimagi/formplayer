package tests;

import beans.NewFormResponse;
import beans.SubmitResponseBean;
import beans.menus.CommandListResponseBean;
import beans.menus.EntityDetailListResponse;
import beans.menus.EntityListResponse;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * Regression tests for fixed behaviors
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class EnikshayEndOfFormTests extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("enikshay_privatedomain", "enikshay_privateusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/enikshay_private.xml";
    }

    @Test
    public void testBadModuleFilter() throws Exception {
        NewFormResponse response =
                sessionNavigate(new String[]{"0", "action 0"},
                "enikshay_private", NewFormResponse.class);
        SubmitResponseBean submitResponse = submitForm(
                    "requests/submit/submit_enikshay_private_0.json",
                    response.getSessionId()
                );

        LinkedHashMap commandsRaw = (LinkedHashMap) submitResponse.getNextScreen();
        String jsonString = new JSONObject(commandsRaw).toString();
        CommandListResponseBean commandResponse = mapper.readValue(jsonString, CommandListResponseBean.class);

        assert commandResponse.getCommands().length == 2;
        assert commandResponse.getCommands()[0].getDisplayText().equals("Manage Beneficiary");
        assert commandResponse.getCommands()[1].getDisplayText().equals("Investigations");
        assert commandResponse.getSelections().length == 2;
        assert commandResponse.getSelections()[0].equals("0");
        assert commandResponse.getBreadcrumbs()[2].equals("NIKITA VERMA");

        ArrayList<String> selections = new ArrayList<>(Arrays.asList(commandResponse.getSelections()));
        selections.add("1");

        String[] newSelections = new String[selections.size()];
        selections.toArray(newSelections);

        EntityListResponse entityResponse = sessionNavigate(
                newSelections,
                "enikshay_private",
                EntityListResponse.class);
        assert entityResponse.getTitle().equals("Investigations");
        assert entityResponse.getEntities().length == 0;
        assert commandResponse.getBreadcrumbs()[2].equals("NIKITA VERMA");
        assert entityResponse.getBreadcrumbs()[3].equals("Investigations");

        selections.add("action 0");
        newSelections = new String[selections.size()];
        selections.toArray(newSelections);

        NewFormResponse formResponse = sessionNavigate(
                newSelections,
                "enikshay_private",
                NewFormResponse.class);
        assert formResponse.getTitle().equals("Order Investigation");
        assert formResponse.getBreadcrumbs().length == 5;

        submitResponse = submitForm(
                "requests/submit/submit_enikshay_private_1.json",
                formResponse.getSessionId()
        );

        commandsRaw = (LinkedHashMap) submitResponse.getNextScreen();
        JSONObject jsonObject = new JSONObject(commandsRaw);
        jsonString = jsonObject.toString();
        entityResponse = mapper.readValue(jsonString, EntityListResponse.class);
        assert entityResponse.getTitle().equals("Investigations");
    }
}
