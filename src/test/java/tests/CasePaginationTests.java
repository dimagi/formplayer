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
import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by willpride on 5/16/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CasePaginationTests extends BaseMenuTestClass {
    @Override
    public void setUp() throws IOException {
        super.setUp();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "restores/ccqa.xml"));
    }

    @Test
    public void testPagination() throws Exception {
        JSONObject sessionNavigateResponse =
                sessionNavigate("requests/navigators/pagination_navigator.json");
        EntityListResponse entityListResponse =
                mapper.readValue(sessionNavigateResponse.toString(), EntityListResponse.class);
        assert entityListResponse.getEntities().length == EntityListResponse.CASE_LENGTH_LIMIT;
        assert entityListResponse.getEntities()[2].getData()[0].equals("Amanda");
        assert entityListResponse.getEntities()[2].getData()[1].equals("27/01/16");
        assert entityListResponse.getCurrentPage() == 0;
        assert entityListResponse.getPageCount() == 8;

        JSONObject sessionNavigateResponse2 =
                sessionNavigate("requests/navigators/pagination_navigator_1.json");
        EntityListResponse entityListResponse2 =
                mapper.readValue(sessionNavigateResponse2.toString(), EntityListResponse.class);
        assert entityListResponse2.getEntities().length == 2;
        assert entityListResponse2.getEntities()[0].getData()[0].equals("clqrk test");
        assert entityListResponse2.getEntities()[0].getData()[1].equals("21/04/16");
        assert entityListResponse2.getCurrentPage() == 7;
        assert entityListResponse2.getPageCount() == 8;
    }

    // test that searching (filtering the case list) works
    @Test
    public void testSearch() throws Exception {
        JSONObject sessionNavigateResponse =
                sessionNavigate("requests/navigators/search_navigator.json");
        EntityListResponse entityListResponse =
                mapper.readValue(sessionNavigateResponse.toString(), EntityListResponse.class);
        assert entityListResponse.getEntities().length == 9;
        assert entityListResponse.getEntities()[0].getData()[0].equals("Casetest");
        assert entityListResponse.getEntities()[1].getData()[0].equals("Test 2");
        assert entityListResponse.getCurrentPage() == 0;
        assert entityListResponse.getPageCount() == 0;
    }
    // test that searching and paginating simultaneously works
    @Test
    public void testSearchAndPagination() throws Exception {
        JSONObject sessionNavigateResponse =
                sessionNavigate("requests/navigators/search_paginate_navigator.json");
        EntityListResponse entityListResponse =
                mapper.readValue(sessionNavigateResponse.toString(), EntityListResponse.class);

        assert entityListResponse.getEntities().length == 10;
        assert entityListResponse.getPageCount() == 2;
        assert entityListResponse.getCurrentPage() == 1;
        assert entityListResponse.getEntities()[0].getData()[0].equals("RESTOSE");
        assert entityListResponse.getEntities()[1].getData()[0].equals("Test 1");
    }
}
