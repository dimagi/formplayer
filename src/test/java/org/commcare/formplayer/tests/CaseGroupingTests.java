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

import java.util.ArrayList;

import javax.annotation.concurrent.Immutable;

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
    public void testCaseListWithGrouping() throws Exception {
        Response<EntityListResponse> response = navigate(new String[]{"1"},
                EntityListResponse.class);
        EntityListResponse entityListResponse = response.bean();
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

    private <T extends BaseResponseBean> Response<T> navigate(
            String[] selections, Class<T> responseClass) {
        String installReference = Installer.getInstallReference(TEST_APP_NAME);
        SessionNavigationRequest<T> request = new SessionNavigationRequest<>(
                mockMvc, responseClass, installReference);
        SessionNavigationBean bean = request.getNavigationBean(selections);
        return request.requestWithBean(bean);
    }
}
