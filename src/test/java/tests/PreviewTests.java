package tests;

import beans.NewFormResponse;
import beans.menus.EntityListResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.FileUtils;
import utils.TestContext;

import java.io.FileInputStream;

import static org.mockito.Mockito.when;

/**
 * @author wspride Test that preview form parameter will work
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class PreviewTests extends BaseTestClass {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("previewdomain", "previewuser");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/ccqa.xml";
    }

    @Test
    public void testPreview() throws Exception {
        EntityListResponse entityListResponse =
                sessionNavigate("requests/preview/preview.json", EntityListResponse.class);
    }

    @Test
    public void testPreviewStep() throws Exception {
        NewFormResponse newFormSessionResponse =
                sessionNavigate("requests/preview/preview_step.json", NewFormResponse.class);
    }
}
