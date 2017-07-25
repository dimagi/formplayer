package tests;

import beans.NewFormResponse;
import beans.SubmitRequestBean;
import beans.SubmitResponseBean;
import beans.menus.CommandListResponseBean;
import beans.menus.EntityDetailListResponse;
import beans.menus.EntityListResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Tests specific to Enikshay
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class Enikshay2bTests extends BaseTestClass {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("enikshay2domain", "enikshay2username");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/enikshay-2b.xml";
    }

    @Test
    public void testEndOfFormNavigation() throws Exception {
        NewFormResponse newFormResponse =
                sessionNavigate(
                        new String[]{"0", "13c960c6-81d9-4606-806f-f685fb24789b",
                                "10", "4edc7248-bfef-4f47-95e0-6b1704981a3f"},
                        "enikshay-2b",
                        NewFormResponse.class);
        SubmitResponseBean submitResponse =
                submitForm("requests/submit/submit_enikshay_2b.json", newFormResponse.getSessionId());
        LinkedHashMap commandsRaw = (LinkedHashMap) submitResponse.getNextScreen();
        String jsonString = new JSONObject(commandsRaw).toString();
        CommandListResponseBean commandResponse = mapper.readValue(jsonString, CommandListResponseBean.class);
        EntityListResponse entityResponse = sessionNavigateWithId(new String[] {"10"},
                commandResponse.getMenuSessionId(),
                EntityListResponse.class);
        NewFormResponse newFormResponse2 = sessionNavigateWithId(new String[] {"10", "4edc7248-bfef-4f47-95e0-6b1704981a3f"},
                commandResponse.getMenuSessionId(),
                NewFormResponse.class);
    }

    @Test
    public void testPersistentCaseTile() throws Exception {
        NewFormResponse newFormResponse =
                sessionNavigate(
                        new String[]{"0", "13c960c6-81d9-4606-806f-f685fb24789b", "0"},
                        "enikshay-2b",
                        NewFormResponse.class);
        assert newFormResponse.getPersistentCaseTile() != null;
        assert newFormResponse.getPersistentCaseTile().getDetails().length == 5;
    }
}
