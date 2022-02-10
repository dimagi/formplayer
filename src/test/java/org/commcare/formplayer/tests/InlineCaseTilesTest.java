package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityDetailListResponse;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Created by willpride on 4/14/16.
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class InlineCaseTilesTest extends BaseTestClass {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("inlinecasetilesdomain", "inlinecasetilesuser");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/inlinecasetiles.xml";
    }

    @Test
    public void testInlineDetails() throws Exception {
        EntityDetailListResponse response =
                getDetailsInline(new String[]{"3", "1d59d16b-c52c-49d6-889e-801736962281"},
                        "inlinecasetiles",
                        EntityDetailListResponse.class);
        assert response.getEntityDetailList().length == 1;
        assert response.getEntityDetailList()[0].getDetails().length == 2;
    }

    @Test
    public void testNoInline() throws Exception {
        CommandListResponseBean response =
                sessionNavigate(new String[]{"1", "1d59d16b-c52c-49d6-889e-801736962281"},
                        "inlinecasetiles",
                        CommandListResponseBean.class);
        assert !response.getPersistentCaseTile().getHasInlineTile();
    }

    @Test
    public void testHasInline() throws Exception {
        CommandListResponseBean response =
                sessionNavigate(new String[]{"3", "1d59d16b-c52c-49d6-889e-801736962281"},
                        "inlinecasetiles",
                        CommandListResponseBean.class);
        assert response.getPersistentCaseTile().getHasInlineTile();
    }
}
