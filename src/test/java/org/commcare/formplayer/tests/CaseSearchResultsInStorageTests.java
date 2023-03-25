package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;

import org.commcare.cases.model.Case;
import org.commcare.formplayer.application.MenuController;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.beans.menus.BaseResponseBean;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.database.models.FormplayerCaseSearchIndexTable;
import org.commcare.formplayer.junit.InitializeStaticsExtension;
import org.commcare.formplayer.junit.Installer;
import org.commcare.formplayer.junit.RestoreFactoryAnswer;
import org.commcare.formplayer.junit.RestoreFactoryExtension;
import org.commcare.formplayer.junit.StorageFactoryExtension;
import org.commcare.formplayer.junit.request.Response;
import org.commcare.formplayer.junit.request.SessionNavigationRequest;
import org.commcare.formplayer.sandbox.CaseSearchSqlSandbox;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.sqlitedb.CaseSearchDB;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.commcare.formplayer.utils.MockRequestUtils;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.web.client.WebClient;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.MD5;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

@WebMvcTest
@Import({MenuController.class})
@ContextConfiguration(classes = {TestContext.class, CacheConfiguration.class})
@ExtendWith(InitializeStaticsExtension.class)
@TestPropertySource(properties = {"formplayer.case_search.min_size_to_store_in_db=0"})
public class CaseSearchResultsInStorageTests {

    @Autowired
    CacheManager cacheManager;

    @Autowired
    protected WebClient webClientMock;

    @Autowired
    RestoreFactory restoreFactoryMock;

    @Autowired
    private MockMvc mockMvc;

    private MockRequestUtils mockRequest;

    @RegisterExtension
    static RestoreFactoryExtension restoreFactoryExt = new RestoreFactoryExtension.builder()
            .withUser("caseclaimuser").withDomain("caseclaimdomain")
            .withRestorePath("restores/caseclaim.xml")
            .build();

    @RegisterExtension
    static StorageFactoryExtension storageExt = new StorageFactoryExtension.builder()
            .withUser("caseclaimuser").withDomain("caseclaimdomain").build();

    @BeforeEach
    public void setUp() throws Exception {
        cacheManager.getCache("case_search").clear();
        mockRequest = new MockRequestUtils(webClientMock, restoreFactoryMock);
    }

    @Test
    public void testCaseClaimWithResultsInStorage() throws Exception {
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_claim_response.xml")) {
            Response<EntityListResponse> response  = navigate(new String[]{"1", "action 1"}, EntityListResponse.class);
            EntityListResponse entityResponse = response.bean();
            assertEquals(entityResponse.getEntities().length,1);

            // Verify the entity and the calculated fields
            EntityBean entity = entityResponse.getEntities()[0];
            assertEquals(entity.getId(),"0156fa3e-093e-4136-b95c-01b13dae66c6");
            assertEquals(entity.getData()[0], "Burt Maclin");
            assertEquals(entity.getData()[1], "Burt Maclin");
        }

        // Verify the results are stored in storage and not cache
        String cacheKey =  "caseclaimdomain_caseclaimuser_http://localhost:8000/a/test/phone/search"
                + "/_case_type=case1=case2=case3_include_closed=False";
        assertNull(cacheManager.getCache("case_search").get(cacheKey));

        // Verify case search storage exists for our query
        SQLiteDB caseSearchDb = new CaseSearchDB("caseclaimdomain", "caseclaimuser", null);
        String caseSearchTableName = MD5.toHex(MD5.hash(cacheKey.getBytes(StandardCharsets.UTF_8)));;
        UserSqlSandbox caseSearchSandbox = new CaseSearchSqlSandbox(caseSearchTableName, caseSearchDb);
        IStorageUtilityIndexed<Case> caseSearchStorage = caseSearchSandbox.getCaseStorage();
        assertTrue(caseSearchStorage.isStorageExists(), "No case storage found for the given case search query");
        FormplayerCaseSearchIndexTable caseSearchIndexTable = new FormplayerCaseSearchIndexTable(
                caseSearchSandbox, caseSearchTableName);
        assertTrue(caseSearchIndexTable.isStorageExists(), "No case storage found for the given case search query");


        // verify the case claim works correctly without mocking the query request again
        // as it should now refetch the data from the search storage
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockPost(true)) {

            // When we sync afterwards, include new case and case-claim
            RestoreFactoryAnswer answer = new RestoreFactoryAnswer("restores/caseclaim2.xml");
            Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml(anyBoolean());

            CommandListResponseBean response = navigate(
                    new String[]{"1", "action 1", "0156fa3e-093e-4136-b95c-01b13dae66c6"},
                    CommandListResponseBean.class).bean();
            assertNotNull(response);
        }

        // verify the case storage has been cleared
        assertFalse(caseSearchStorage.isStorageExists(), "Case search storage has not been cleared after case claim");
        assertFalse(caseSearchIndexTable.isStorageExists(), "Case Indexes have not been cleared after case claim");
    }

    private <T extends BaseResponseBean> Response<T> navigate(
            String[] selections, Class<T> responseClass) {
        String installReference = Installer.getInstallReference("caseclaim");
        SessionNavigationRequest<T> request = new SessionNavigationRequest<>(
                mockMvc, responseClass, installReference);
        SessionNavigationBean bean = request.getNavigationBean(selections);
        return request.requestWithBean(bean);
    }
}
