package tests;

import beans.menus.CommandListResponseBean;
import beans.menus.EntityDetailListResponse;
import beans.menus.EntityListResponse;
import beans.menus.Tile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

/**
 * Created by willpride on 4/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class InlineCaseTilesTest extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("inlinecasetilesdomain", "inlinecasetilesuser");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/inlinecasetiles.xml";
    }

    @Test
    public void testCaseTiles() throws Exception {
        EntityDetailListResponse response =
                getDetailsInline(new String[] {"1", "1d59d16b-c52c-49d6-889e-801736962281"},
                        "inlinecasetiles",
                        EntityDetailListResponse.class);
        assert response.getEntityDetailList().length == 2;
    }
}