package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.PersistentCommand;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.HashMap;

@WebMvcTest
public class BasicEndOfFormTests extends BaseTestClass {

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
        CommandListResponseBean commandResponse = getNextScreenForEofNavigation(submitResponse,
                CommandListResponseBean.class);
        assert commandResponse.getCommands().length == 19;
        assert commandResponse.getTitle().equals("Basic Tests");

        ArrayList<PersistentCommand> persistentMenu = commandResponse.getPersistentMenu();
        assertEquals(19, persistentMenu.size());
        assertEquals("End of Form Navigation", persistentMenu.get(15).getDisplayText());
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
        EntityListResponse entityResponse = getNextScreenForEofNavigation(submitResponse,
                EntityListResponse.class);
        assert entityResponse.getEntities().length == 3;
        assert entityResponse.getTitle().equals("Previous Screen");

        ArrayList<PersistentCommand> persistentMenu = entityResponse.getPersistentMenu();
        assertEquals(19, persistentMenu.size());
        PersistentCommand eofMenu = persistentMenu.get(15);
        assertEquals("End of Form Navigation", eofMenu.getDisplayText());
        assertEquals(5, eofMenu.getCommands().size());
        assertEquals("Previous Screen", eofMenu.getCommands().get(2).getDisplayText());
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
        CommandListResponseBean commandResponse = getNextScreenForEofNavigation(submitResponse,
                CommandListResponseBean.class);
        assert commandResponse.getCommands().length == 5;
        assert commandResponse.getTitle().equals("End of Form Navigation");

        ArrayList<PersistentCommand> persistentMenu = commandResponse.getPersistentMenu();
        assertEquals(19, persistentMenu.size());
        PersistentCommand eofMenu = persistentMenu.get(15);
        assertEquals("End of Form Navigation", eofMenu.getDisplayText());
        assertEquals(5, eofMenu.getCommands().size());
        assertEquals("Current Module", eofMenu.getCommands().get(3).getDisplayText());
    }

    @Test
    public void testCloseCase() throws Exception {
        NewFormResponse response =
                sessionNavigate(new String[]{"15", "4", "17967331-5ef3-40a7-bcb4-ee36fb9091c2"},
                        "basic", NewFormResponse.class);
        SubmitResponseBean submitResponse = submitForm(response.getSessionId());
        NewFormResponse formResponse = getNextScreenForEofNavigation(submitResponse,
                NewFormResponse.class);
        assert formResponse.getTitle().equals("Home Screen");
        assert formResponse.getTree().length == 1;

        ArrayList<PersistentCommand> persistentMenu = formResponse.getPersistentMenu();
        assertEquals(19, persistentMenu.size());
        PersistentCommand eofMenu = persistentMenu.get(15);
        assertEquals("End of Form Navigation", eofMenu.getDisplayText());
        assertEquals(5, eofMenu.getCommands().size());
        assertEquals("Close Case", eofMenu.getCommands().get(4).getDisplayText());
    }
}
