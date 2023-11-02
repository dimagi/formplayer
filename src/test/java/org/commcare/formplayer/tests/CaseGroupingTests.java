package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;

import org.commcare.formplayer.application.MenuController;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.beans.menus.BaseResponseBean;
import org.commcare.formplayer.beans.menus.EntityBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.junit.InitializeStaticsExtension;
import org.commcare.formplayer.junit.Installer;
import org.commcare.formplayer.junit.RestoreFactoryExtension;
import org.commcare.formplayer.junit.StorageFactoryExtension;
import org.commcare.formplayer.junit.request.Response;
import org.commcare.formplayer.junit.request.SessionNavigationRequest;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.utils.MockRequestUtils;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.web.client.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import({MenuController.class})
@ContextConfiguration(classes = {TestContext.class, CacheConfiguration.class})
@ExtendWith(InitializeStaticsExtension.class)
public class CaseGroupingTests {
    private static final String TEST_APP_NAME = "case_list_auto_select";

    @Autowired
    protected WebClient webClientMock;

    @Autowired
    RestoreFactory restoreFactoryMock;

    @Autowired
    private MockMvc mockMvc;

    private MockRequestUtils mockRequest;


    @Autowired
    FormplayerStorageFactory storageFactoryMock;

    @RegisterExtension
    static RestoreFactoryExtension restoreFactoryExt = new RestoreFactoryExtension.builder()
            .withUser("caseclaimuser").withDomain("caseclaimdomain")
            .withRestorePath("restores/casegroup.xml")
            .build();

    @RegisterExtension
    static StorageFactoryExtension storageExt = new StorageFactoryExtension.builder()
            .withUser("caseclaimuser").withDomain("caseclaimdomain").build();

    @BeforeEach
    public void setUp() throws Exception {
        mockRequest = new MockRequestUtils(webClientMock, restoreFactoryMock);
    }

    @Test
    public void testCaseListWithGrouping() {
        EntityListResponse entityListResponse = loadCaseList( 0, 100);
        assertEquals(entityListResponse.getGroupHeaderRows(), 2);
        EntityBean[] entities = entityListResponse.getEntities();
        assertTrue(entities.length == 8);
        // confirm the order of entities by group
        ImmutableList<String> expectedIds = ImmutableList.of("case1", "case3", "case6", "case2", "case5", "case8",
                "case4", "case7");
        ImmutableList<String> expectedGroupKeys = ImmutableList.of("parentB", "parentB", "parentB", "parentA",
                "parentA", "parentA", "parentC", "parentC");
        for (int i = 0; i < entities.length; i++) {
            assertEquals(expectedIds.get(i), entities[i].getId());
            assertEquals(expectedGroupKeys.get(i), entities[i].getGroupKey());
        }
    }

    @Test
    public void testCaseListPaginationWithGrouping() {
        EntityListResponse entityListResponse= loadCaseList( 0, 2);
        assertTrue(entityListResponse.getCurrentPage() == 0);
        assertTrue(entityListResponse.getPageCount() == 2);
        assertTrue(entityListResponse.getEntities().length == 6);
        ImmutableList<String> expectedIds = ImmutableList.of("case1", "case3", "case6", "case2", "case5", "case8");
        matchEntityIds(expectedIds, entityListResponse.getEntities());

        entityListResponse= loadCaseList( 1, 2);
        assertTrue(entityListResponse.getCurrentPage() == 0);
        assertTrue(entityListResponse.getPageCount() == 2);
        assertTrue(entityListResponse.getEntities().length == 5);
        expectedIds = ImmutableList.of( "case2", "case5", "case8", "case4", "case7");
        matchEntityIds(expectedIds, entityListResponse.getEntities());

        entityListResponse= loadCaseList( 2, 2);
        assertTrue(entityListResponse.getCurrentPage() == 1);
        assertTrue(entityListResponse.getPageCount() == 2);
        assertTrue(entityListResponse.getEntities().length == 2);
        expectedIds = ImmutableList.of("case4", "case7");
        matchEntityIds(expectedIds, entityListResponse.getEntities());

        entityListResponse= loadCaseList( 1, 1);
        assertTrue(entityListResponse.getCurrentPage() == 1);
        assertTrue(entityListResponse.getPageCount() == 3);
        assertTrue(entityListResponse.getEntities().length == 3);
        expectedIds = ImmutableList.of("case2", "case5", "case8");
        matchEntityIds(expectedIds, entityListResponse.getEntities());
    }

    private void matchEntityIds(ImmutableList<String> expectedIds, EntityBean[] entities) {
        for (int i = 0; i < entities.length; i++) {
            assertEquals(expectedIds.get(i), entities[i].getId());
        }
    }

    private EntityListResponse loadCaseList(int offset, int groupsPerPage) {
        String[] selections = new String[]{"1"};
        String installReference = Installer.getInstallReference(TEST_APP_NAME);
        SessionNavigationRequest<EntityListResponse> request = new SessionNavigationRequest<>(
                mockMvc, EntityListResponse.class, installReference);
        SessionNavigationBean bean = request.getNavigationBean(selections);
        bean.setCasesPerPage(groupsPerPage);
        bean.setOffset(offset);
        return request.requestWithBean(bean).bean();
    }
}
