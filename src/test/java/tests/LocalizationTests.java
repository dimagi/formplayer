package tests;

import auth.HqAuth;
import beans.NewFormResponse;
import beans.menus.CommandListResponseBean;
import java.io.IOException;
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

    public void setUp() throws IOException {
        super.setUp();
        Mockito.when(this.restoreServiceMock.getRestoreXml(Matchers.anyString(),
                Matchers.any(HqAuth.class))).thenReturn(FileUtils.getFile(this.getClass(), "restores/ccqa.xml"));
    }

    @Test
    public void testMenuLocalization() throws Exception {
        JSONObject sessionNavigateResponse = this.sessionNavigate(new String[]{"0"}, "langs");
        CommandListResponseBean commandListResponseBean =
                this.mapper.readValue(sessionNavigateResponse.toString(), CommandListResponseBean.class);

        assert commandListResponseBean.getCommands().length == 2;

        assert commandListResponseBean.getCommands()[0].getDisplayText().equals("English Form 1");

        assert commandListResponseBean.getCommands()[1].getDisplayText().equals("English Form 2");

        JSONObject sessionNavigateResponseSpanish = this.sessionNavigate(new String[]{"0"}, "langs", "es");
        CommandListResponseBean commandListResponseSpanish =
                this.mapper.readValue(sessionNavigateResponseSpanish.toString(), CommandListResponseBean.class);

        assert commandListResponseSpanish.getCommands().length == 2;

        assert commandListResponseSpanish.getLocales().length == 3;

        assert commandListResponseSpanish.getCommands()[0].getDisplayText().equals("Spanish Form 1");

        assert commandListResponseSpanish.getCommands()[1].getDisplayText().equals("Spanish Form 2");

    }

    @Test
    public void testFormLocalization() throws Exception {
        JSONObject sessionNavigateResponse = this.sessionNavigate(new String[]{"0", "0"}, "langs");
        NewFormResponse newFormResponse =
                this.mapper.readValue(sessionNavigateResponse.toString(), NewFormResponse.class);

        assert newFormResponse.getTree().length == 2;

        assert newFormResponse.getTree()[0].getCaption().equals("I'm English");

        assert newFormResponse.getTree()[1].getCaption().equals("English rules");

        JSONObject sessionNavigateResponseSpanish = this.sessionNavigate(new String[]{"0", "0"}, "langs", "es");
        NewFormResponse newFormResponseSpanish =
                this.mapper.readValue(sessionNavigateResponseSpanish.toString(), NewFormResponse.class);

        assert newFormResponseSpanish.getTree().length == 2;

        assert newFormResponseSpanish.getTree()[0].getCaption().equals("I'm Spanish");

        assert newFormResponseSpanish.getTree()[1].getCaption().equals("No Spanish rules");

    }
}
