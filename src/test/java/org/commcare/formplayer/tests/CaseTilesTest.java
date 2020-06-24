package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.Tile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.commcare.formplayer.utils.TestContext;

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