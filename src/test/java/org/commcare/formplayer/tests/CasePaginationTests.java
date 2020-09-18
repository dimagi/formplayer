package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.menus.EntityBean;
import org.commcare.formplayer.beans.menus.EntityDetailListResponse;
import org.commcare.formplayer.beans.menus.EntityDetailResponse;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.Property;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.commcare.formplayer.mocks.FormPlayerPropertyManagerMock;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.commcare.formplayer.utils.TestContext;
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
        configureRestoreFactory("loaddomain", "loaduser");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/ccqa.xml";
    }

    @Test
    public void testSort() throws Exception {
        EntityListResponse entityListResponse =
                sessionNavigate("requests/navigators/pagination_navigator.json", EntityListResponse.class);
        assert entityListResponse.getEntities().length == EntityListResponse.CASE_LENGTH_LIMIT;
        String previousName = "";
        for (EntityBean entity: entityListResponse.getEntities()) {
            String currentName = (String) entity.getData()[0];
            assert previousName.compareTo(currentName) < 0;
            previousName = currentName;
        }
    }

    @Test
    public void testPagination() throws Exception {
        EntityListResponse entityListResponse =
                sessionNavigate("requests/navigators/pagination_navigator.json", EntityListResponse.class);
        assert entityListResponse.getEntities().length == EntityListResponse.CASE_LENGTH_LIMIT;
        assert entityListResponse.getCurrentPage() == 0;
        assert entityListResponse.getPageCount() == 8;

        EntityDetailListResponse details =
                getDetails("requests/get_details/pagination_navigator_details.json", EntityDetailListResponse.class);

        assert details.getEntityDetailList().length == 2;

        EntityListResponse entityListResponse2 =
                sessionNavigate("requests/navigators/pagination_navigator_1.json", EntityListResponse.class);
        assert entityListResponse2.getEntities().length == 2;
        assert entityListResponse2.getCurrentPage() == 7;
        assert entityListResponse2.getPageCount() == 8;

        EntityDetailListResponse details2 =
                getDetails("requests/get_details/pagination_navigator_details_1.json", EntityDetailListResponse.class);

        assert details2.getEntityDetailList().length == 2;
        EntityDetailResponse firstDetail = details2.getEntityDetailList()[0];
        EntityDetailResponse secondDetail = details2.getEntityDetailList()[1];

        assert firstDetail.getDetails().length == 3;
        assert secondDetail.getDetails().length == 0;

        assert firstDetail.getHeaders()[0].equals("Name");
    }

    // test that searching (filtering the case list) works
    @Test
    public void testFuzzySearch() throws Exception {
        EntityListResponse entityListResponse =
                sessionNavigate("requests/navigators/search_navigator.json", EntityListResponse.class);
        assert entityListResponse.getEntities().length == 10;
        assert entityListResponse.getCurrentPage() == 0;
        assert entityListResponse.getPageCount() == 0;
    }

    @Test
    public void testNormalSearch() throws Exception {
        SQLiteDB db = storageFactoryMock.getSQLiteDB();
        FormPlayerPropertyManagerMock propertyManagerMock = new FormPlayerPropertyManagerMock(new SqlStorage(db, Property.class, PropertyManager.STORAGE_KEY));
        propertyManagerMock.enableFuzzySearch(false);
        when(storageFactoryMock.getPropertyManager()).thenReturn(propertyManagerMock);
        EntityListResponse entityListResponse =
                sessionNavigate("requests/navigators/search_navigator.json", EntityListResponse.class);
        assert entityListResponse.getEntities().length == 9;
        assert entityListResponse.getCurrentPage() == 0;
        assert entityListResponse.getPageCount() == 0;
        db.closeConnection();
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
