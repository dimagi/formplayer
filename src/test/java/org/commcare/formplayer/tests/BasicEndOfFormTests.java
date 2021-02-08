package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.utils.TestContext;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class BasicEndOfFormTests extends BaseTestClass{

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("basic_eofdomain", "basic_eofusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/basic.xml";
    }

    private HashMap<String, Object> getAnswers(String index, String answer) {
        HashMap<String, Object> ret = new HashMap<>();
        ret.put(index, answer);
        return ret;
    }

    <T> T getNextScreen(SubmitResponseBean submitResponse, Class<T> clazz) throws IOException {
        LinkedHashMap commandsRaw = (LinkedHashMap) submitResponse.getNextScreen();
        String jsonString = new JSONObject(commandsRaw).toString();
        return mapper.readValue(jsonString, clazz);
    }


    @Test
    public void testHomeScreen() throws Exception {
        NewFormResponse response =
                sessionNavigate(new String[]{"15", "0"},
                        "basic", NewFormResponse.class);
        SubmitResponseBean submitResponse = submitForm(
                getAnswers("0", "0"),
                response.getSessionId()
        );
        assert submitResponse.getNextScreen() == null;
    }

    @Test
    public void testModuleScreen() throws Exception {
        NewFormResponse response =
                sessionNavigate(new String[]{"15", "1"},
                        "basic", NewFormResponse.class);
        SubmitResponseBean submitResponse = submitForm(
                getAnswers("0", "0"),
                response.getSessionId()
        );
        CommandListResponseBean commandResponse = getNextScreen(submitResponse, CommandListResponseBean.class);
        assert commandResponse.getCommands().length == 18;
        assert commandResponse.getTitle().equals("Basic Tests");
    }

    @Test
    public void testPreviousScreen() throws Exception {
        NewFormResponse response =
                sessionNavigate(new String[]{"15", "2", "bef97ca3-e815-4841-ae54-0baf40d9f1af"},
                "basic", NewFormResponse.class);
        SubmitResponseBean submitResponse = submitForm(
                    getAnswers("0", "0"),
                    response.getSessionId()
                );
        EntityListResponse entityResponse = getNextScreen(submitResponse, EntityListResponse.class);
        assert entityResponse.getEntities().length == 3;
        assert entityResponse.getTitle().equals("Previous Screen");
    }

    @Test
    public void testCurrentModule() throws Exception {
        NewFormResponse response =
                sessionNavigate(new String[]{"15", "3"},
                        "basic", NewFormResponse.class);
        SubmitResponseBean submitResponse = submitForm(
                getAnswers("0", "0"),
                response.getSessionId()
        );
        CommandListResponseBean commandResponse = getNextScreen(submitResponse, CommandListResponseBean.class);
        assert commandResponse.getCommands().length == 5;
        assert commandResponse.getTitle().equals("End of Form Navigation");
    }

    @Test
    public void testCloseCase() throws Exception {
        NewFormResponse response =
                sessionNavigate(new String[]{"15", "4", "17967331-5ef3-40a7-bcb4-ee36fb9091c2"},
                        "basic", NewFormResponse.class);
        SubmitResponseBean submitResponse = submitForm(response.getSessionId());
        NewFormResponse formResponse = getNextScreen(submitResponse, NewFormResponse.class);
        assert formResponse.getTitle().equals("Home Screen");
        assert formResponse.getTree().length == 1;
    }
}
