package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.commcare.formplayer.beans.menus.EntityBean;
import org.commcare.formplayer.beans.menus.EntityDetailListResponse;
import org.commcare.formplayer.beans.menus.EntityDetailResponse;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.mocks.FormPlayerPropertyManagerMock;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

/**
 * Created by willpride on 5/16/16.
 */
@WebMvcTest
public class CasePaginationTests extends BaseTestClass {
    @Override
    @BeforeEach
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
                sessionNavigate("requests/navigators/pagination_navigator.json",
                        EntityListResponse.class);
        assert entityListResponse.getEntities().length == 20;
        String previousName = "";
        for (EntityBean entity : entityListResponse.getEntities()) {
            String currentName = (String)entity.getData()[0];
            // Case list sort converts text values to lowercase before comparing
            assert previousName.toLowerCase().compareTo(currentName.toLowerCase()) <= 0;
            previousName = currentName;
        }
    }

    @Test
    public void testPagination() throws Exception {
        EntityListResponse entityListResponse =
                sessionNavigate("requests/navigators/pagination_navigator.json",
                        EntityListResponse.class);
        assert entityListResponse.getEntities().length == 20;
        assert entityListResponse.getCurrentPage() == 0;
        assert entityListResponse.getPageCount() == 4;

        EntityDetailListResponse details =
                getDetails("requests/get_details/pagination_navigator_details.json",
                        EntityDetailListResponse.class);

        assert details.getEntityDetailList().length == 2;
        checkDetailTemplates(details.getEntityDetailList()[0]);

        EntityListResponse entityListResponse2 =
                sessionNavigate("requests/navigators/pagination_navigator_1.json",
                        EntityListResponse.class);
        EntityBean[] responseEntities = entityListResponse2.getEntities();
        assert responseEntities.length == 12;
        assert entityListResponse2.getCurrentPage() == 4;
        assert entityListResponse2.getPageCount() == 5;

        // check the order of entities is correct
        assertEquals(responseEntities[0].getData()[0], "Test 1");
        assertEquals(responseEntities[11].getData()[0], "Yoi");

        EntityDetailListResponse details2 =
                getDetails("requests/get_details/pagination_navigator_details_1.json",
                        EntityDetailListResponse.class);

        assert details2.getEntityDetailList().length == 2;
        EntityDetailResponse firstDetail = details2.getEntityDetailList()[0];
        EntityDetailResponse secondDetail = details2.getEntityDetailList()[1];

        assert firstDetail.getDetails().length == 3;
        assert secondDetail.getDetails().length == 0;

        assert firstDetail.getHeaders()[0].equals("Name");
    }

    private void checkDetailTemplates(EntityDetailResponse entityDetailResponse) {
        assert String.valueOf(entityDetailResponse.getStyles()[3].getDisplayFormat()).equals(
                "Markdown");
        assert String.valueOf(entityDetailResponse.getStyles()[4].getDisplayFormat()).equals(
                "Phone");
    }

    // test that searching (filtering the case list) works
    @Test
    public void testFuzzySearch() throws Exception {
        EntityListResponse entityListResponse =
                sessionNavigate("requests/navigators/search_navigator.json",
                        EntityListResponse.class);
        assert entityListResponse.getEntities().length == 10;
        assert entityListResponse.getCurrentPage() == 0;
        assert entityListResponse.getPageCount() == 0;
    }

    @Test
    public void testNormalSearch() throws Exception {
        SQLiteDB db = storageFactoryMock.getSQLiteDB();
        FormPlayerPropertyManagerMock propertyManagerMock = new FormPlayerPropertyManagerMock(
                new SqlStorage(db, Property.class, PropertyManager.STORAGE_KEY));
        propertyManagerMock.enableFuzzySearch(false);
        when(storageFactoryMock.getPropertyManager()).thenReturn(propertyManagerMock);
        EntityListResponse entityListResponse =
                sessionNavigate("requests/navigators/search_navigator.json",
                        EntityListResponse.class);
        assert entityListResponse.getEntities().length == 9;
        assert entityListResponse.getCurrentPage() == 0;
        assert entityListResponse.getPageCount() == 0;
        db.closeConnection();
    }

    // test that searching and paginating simultaneously works
    @Test
    public void testSearchAndPagination() throws Exception {
        EntityListResponse entityListResponse =
                sessionNavigate("requests/navigators/search_paginate_navigator.json",
                        EntityListResponse.class);
        EntityBean[] responseEntities = entityListResponse.getEntities();
        assert responseEntities.length == 5;
        assert entityListResponse.getPageCount() == 2;
        assert entityListResponse.getCurrentPage() == 1;

        // check the order of entities is correct
        assertEquals(responseEntities[0].getData()[0], "Test");
        assertEquals(responseEntities[4].getData()[0], "Test123456");
    }
}
