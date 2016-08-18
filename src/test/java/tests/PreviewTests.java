package tests;

import auth.HqAuth;
import beans.NewFormSessionResponse;
import beans.menus.BaseResponseBean;
import beans.menus.Entity;
import beans.menus.EntityDetailResponse;
import beans.menus.EntityListResponse;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;
import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author wspride Test that preview form parameter will work
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class PreviewTests extends BaseTestClass {
    @Override
    public void setUp() throws IOException {
        super.setUp();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "restores/ccqa.xml"));
    }

    @Test
    public void testPreview() throws Exception {
        JSONObject previewForm =
                sessionNavigate("requests/preview/preview.json");
        EntityListResponse entityListResponse =
                mapper.readValue(previewForm.toString(), EntityListResponse.class);
    }

    @Test
    public void testPreviewStep() throws Exception {
        JSONObject previewForm =
                sessionNavigate("requests/preview/preview_step.json");
        NewFormSessionResponse newFormSessionResponse =
                mapper.readValue(previewForm.toString(), NewFormSessionResponse.class);
    }
}
