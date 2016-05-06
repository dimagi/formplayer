package tests;

import auth.HqAuth;
import beans.menus.EntityListResponse;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by willpride on 5/6/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CaseLoadTest extends BaseMenuTestClass{

    @Override
    public void setUp() throws IOException {
        super.setUp();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "restores/big_restore.xml"));
    }

    @Test
    public void testLoadLargeCaseList() throws Exception {

        // Start new session and submit create case form

        JSONObject response = sessionNavigate("requests/navigators/load_navigator_0.json");
        EntityListResponse caseListResponse =
                mapper.readValue(response.toString(), EntityListResponse.class);
        assert caseListResponse.getEntities().length == 1246;
    }
}
