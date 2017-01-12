package tests;

import auth.HqAuth;
import beans.NewFormResponse;
import beans.menus.CommandListResponseBean;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.FileUtils;
import utils.TestContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes = {TestContext.class}
)
public class LocalizationTests extends BaseTestClass {
    public LocalizationTests() {
    }

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
                this.sessionNavigate(new String[]{"0"}, "langs", "es", CommandListResponseBean.class);

        assert commandListResponseSpanish.getCommands().length == 2;

        assert commandListResponseSpanish.getLocales().length == 3;

        assert commandListResponseSpanish.getCommands()[0].getDisplayText().equals("Spanish Form 1");

        assert commandListResponseSpanish.getCommands()[1].getDisplayText().equals("Spanish Form 2");

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
