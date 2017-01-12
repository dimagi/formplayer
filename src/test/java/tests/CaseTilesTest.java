package tests;

import auth.HqAuth;
import beans.menus.BaseResponseBean;
import beans.menus.CommandListResponseBean;
import beans.menus.EntityListResponse;
import beans.menus.Tile;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by willpride on 4/14/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CaseTilesTest extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("casetilesdomain", "casetilesuser");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/casetiles.xml";
    }

    @Test
    public void testCaseTiles() throws Exception {
        EntityListResponse response =
                sessionNavigate(new String[] {"2"}, "casetiles", EntityListResponse.class);
        assert response.getUsesCaseTiles();
        Tile[] tiles = response.getTiles();
        assert tiles.length == 14;
        assert tiles[13].getFontSize().equals("small");
        assert tiles[0].getGridX() == 0;
        assert tiles[0].getGridY() == 0;
        assert tiles[1].getGridHeight() == 1;
        assert tiles[1].getGridWidth() == 7;
    }

    @Test
    public void testPersistentCaseTile() throws Exception {
        CommandListResponseBean response =
                sessionNavigate(new String[] {"2", "bf1fc10c-ec65-4af7-b2a4-aa38dcb7af0c"}, "casetiles", CommandListResponseBean.class);
        assert response.getCommands().length == 5;
        assert response.getPersistentCaseTile().isUsesCaseTiles();
        assert response.getPersistentCaseTile().getTiles().length == 14;
    }
}