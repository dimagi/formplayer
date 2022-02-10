package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.FormEntryResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

@WebMvcTest
@ContextConfiguration(
        classes = {TestContext.class}
)
public class LocalizationTests extends BaseTestClass {
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("langsdomain", "langsuser");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/ccqa.xml";
    }

    @Test
    public void testMenuLocalization() throws Exception {
        CommandListResponseBean commandListResponseBean =
                this.sessionNavigate(new String[]{"0"}, "langs", CommandListResponseBean.class);

        assert commandListResponseBean.getCommands().length == 2;

        assert commandListResponseBean.getCommands()[0].getDisplayText().equals("English Form 1");

        assert commandListResponseBean.getCommands()[1].getDisplayText().equals("English Form 2");

        CommandListResponseBean commandListResponseSpanish =
                this.sessionNavigate(new String[]{"0"}, "langs", "es",
                        CommandListResponseBean.class);

        assert commandListResponseSpanish.getCommands().length == 2;

        assert commandListResponseSpanish.getLocales().length == 3;

        assert commandListResponseSpanish.getCommands()[0].getDisplayText().equals(
                "Spanish Form 1");

        assert commandListResponseSpanish.getCommands()[1].getDisplayText().equals(
                "Spanish Form 2");

    }

    @Test
    public void testInFormLocalization() throws Exception {
        NewFormResponse newFormResponse =
                this.sessionNavigate(new String[]{"0", "0"}, "langs", NewFormResponse.class);

        assert newFormResponse.getTree().length == 2;

        assert newFormResponse.getTree()[0].getCaption().equals("I'm English");

        assert newFormResponse.getTree()[1].getCaption().equals("English rules");

        FormEntryResponseBean formResponse = this.changeLanguage("es",
                newFormResponse.getSessionId());

        assert formResponse.getTree()[0].getCaption().equals("I'm Spanish");

        assert formResponse.getTree()[1].getCaption().equals("No Spanish rules");

        formResponse = this.answerQuestionGetResult(null, "0", newFormResponse.getSessionId());

        assert formResponse.getTree()[0].getCaption().equals("I'm Spanish");

        assert formResponse.getTree()[1].getCaption().equals("No Spanish rules");

    }

    @Test
    public void testFormLocalization() throws Exception {
        NewFormResponse newFormResponse =
                this.sessionNavigate(new String[]{"0", "0"}, "langs", NewFormResponse.class);

        assert newFormResponse.getTree().length == 2;

        assert newFormResponse.getTree()[0].getCaption().equals("I'm English");

        assert newFormResponse.getTree()[1].getCaption().equals("English rules");

        NewFormResponse newFormResponseSpanish =
                this.sessionNavigate(new String[]{"0", "0"}, "langs", "es", NewFormResponse.class);

        assert newFormResponseSpanish.getTree().length == 2;

        assert newFormResponseSpanish.getTree()[0].getCaption().equals("I'm Spanish");

        assert newFormResponseSpanish.getTree()[1].getCaption().equals("No Spanish rules");

    }
}
