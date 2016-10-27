package tests;

import beans.menus.EntityDetailResponse;
import beans.menus.EntityListResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.FileUtils;
import utils.TestContext;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by willpride on 5/16/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CasePaginationTests extends BaseTestClass {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(restoreFactoryMock.getRestoreXml())
                .thenReturn(FileUtils.getFile(this.getClass(), "restores/ccqa.xml"));
        configureRestoreFactory("loaddomain", "loaduser");
    }

    @Test
    public void testPagination() throws Exception {
        EntityListResponse entityListResponse =
                sessionNavigate("requests/navigators/pagination_navigator.json", EntityListResponse.class);
        assert entityListResponse.getEntities().length == EntityListResponse.CASE_LENGTH_LIMIT;
        assert entityListResponse.getCurrentPage() == 0;
        assert entityListResponse.getPageCount() == 8;
        assert entityListResponse.getEntities()[0].getDetails().length == 2;

        EntityListResponse entityListResponse2 =
                sessionNavigate("requests/navigators/pagination_navigator_1.json", EntityListResponse.class);
        assert entityListResponse2.getEntities().length == 2;
        assert entityListResponse2.getCurrentPage() == 7;
        assert entityListResponse2.getPageCount() == 8;

        assert entityListResponse2.getEntities()[0].getDetails().length == 2;
        EntityDetailResponse firstDetail = entityListResponse2.getEntities()[0].getDetails()[0];
        EntityDetailResponse secondDetail = entityListResponse2.getEntities()[0].getDetails()[1];

        assert firstDetail.getDetails().length == 4;
        assert secondDetail.getDetails().length == 6;

        assert firstDetail.getHeaders()[0].equals("Name");
        assert secondDetail.getHeaders()[2].equals("Intval");
    }

    // test that searching (filtering the case list) works
    @Test
    public void testSearch() throws Exception {
        EntityListResponse entityListResponse =
                sessionNavigate("requests/navigators/search_navigator.json", EntityListResponse.class);
        assert entityListResponse.getEntities().length == 9;
        assert entityListResponse.getCurrentPage() == 0;
        assert entityListResponse.getPageCount() == 0;
    }
    // test that searching and paginating simultaneously works
    @Test
    public void testSearchAndPagination() throws Exception {
        EntityListResponse entityListResponse =
                sessionNavigate("requests/navigators/search_paginate_navigator.json", EntityListResponse.class);
        assert entityListResponse.getEntities().length == 10;
        assert entityListResponse.getPageCount() == 2;
        assert entityListResponse.getCurrentPage() == 1;
    }
}
