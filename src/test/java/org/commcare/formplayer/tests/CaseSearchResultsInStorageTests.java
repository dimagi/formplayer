package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import org.commcare.cases.model.Case;
import org.commcare.formplayer.application.MenuController;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.beans.menus.BaseResponseBean;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.database.models.FormplayerCaseIndexTable;
import org.commcare.formplayer.engine.FormplayerConfigEngine;
import org.commcare.formplayer.junit.InitializeStaticsExtension;
import org.commcare.formplayer.junit.Installer;
import org.commcare.formplayer.junit.RestoreFactoryAnswer;
import org.commcare.formplayer.junit.RestoreFactoryExtension;
import org.commcare.formplayer.junit.StorageFactoryExtension;
import org.commcare.formplayer.junit.request.Response;
import org.commcare.formplayer.junit.request.SessionNavigationRequest;
import org.commcare.formplayer.mocks.FormPlayerPropertyManagerMock;
import org.commcare.formplayer.sandbox.CaseSearchSqlSandbox;
import org.commcare.formplayer.sandbox.SqlSandboxUtils;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.InstallService;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.sqlitedb.CaseSearchDB;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.commcare.formplayer.utils.MockRequestUtils;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.web.client.WebClient;
import org.commcare.resources.model.installers.SuiteInstaller;
import org.commcare.suite.model.Suite;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.Property;
import org.javarosa.core.services.storage.IStorageIterator;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Tests for case search workflows with cc-index-case-search-results on
 */
@WebMvcTest
@Import({MenuController.class})
@ContextConfiguration(classes = {TestContext.class, CacheConfiguration.class})
@ExtendWith(InitializeStaticsExtension.class)
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


    @Autowired
    FormplayerStorageFactory storageFactoryMock;

    @Autowired
    InstallService installService;

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
        FileSystemUtils.deleteRecursively(new File("tmp_dbs"));
        enableIndexCaseSearchResults();
    }

    private void enableIndexCaseSearchResults() {
        SQLiteDB db = storageFactoryMock.getSQLiteDB();
        FormPlayerPropertyManagerMock propertyManagerMock = new FormPlayerPropertyManagerMock(
                new SqlStorage(db, Property.class, PropertyManager.STORAGE_KEY));
        propertyManagerMock.enableIndexCaseSearchResults(true);
        when(storageFactoryMock.getPropertyManager()).thenReturn(propertyManagerMock);
    }

    @Test
    public void testCaseClaimWithResultsInStorage() throws Exception {
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_claim_response.xml")) {
            Response<EntityListResponse> response = navigate(new String[]{"1", "action 1"},
                    EntityListResponse.class);
            EntityListResponse entityResponse = response.bean();
            assertEquals(entityResponse.getEntities().length, 1);

            // Verify the entity and the calculated fields
            EntityBean entity = entityResponse.getEntities()[0];
            assertEquals(entity.getId(), "0156fa3e-093e-4136-b95c-01b13dae66c6");
            assertEquals(entity.getData()[0], "Burt Maclin");
            assertEquals(entity.getData()[1], "Burt Maclin");
            assertEquals(entity.getData()[2], "Kurt Maclin");
        }

        // Verify the results are stored in storage and not cache
        String cacheKey = "caseclaimdomain_caseclaimuser_http://localhost:8000/a/test/phone/search"
                + "/_case_type=case1=case2=case3_include_closed=False_x_commcare_tag_module_name=Search All Cases";
        assertNull(cacheManager.getCache("case_search").get(cacheKey));

        // Verify case search storage exists for our query
        SQLiteDB caseSearchDb = new CaseSearchDB("caseclaimdomain", "caseclaimuser", null);
        String caseSearchTableName = getCaseSearchTableName(cacheKey);
        UserSqlSandbox caseSearchSandbox = new CaseSearchSqlSandbox(caseSearchTableName, caseSearchDb);
        IStorageUtilityIndexed<Case> caseSearchStorage = caseSearchSandbox.getCaseStorage();
        assertTrue(caseSearchStorage.isStorageExists(), "No case storage found for the given case search query");
        FormplayerCaseIndexTable caseSearchIndexTable = getCaseIndexTable(caseSearchSandbox, caseSearchTableName);
        assertTrue(caseSearchIndexTable.isStorageExists(),
                "No case storage found for the given case search query");


        // verify the case claim works correctly without mocking the query request again
        // as it should now refetch the data from the search storage
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockPost(true)) {

            // When we sync afterwards, include new case and case-claim
            RestoreFactoryAnswer answer = new RestoreFactoryAnswer("restores/caseclaim2.xml");
            Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml();

            CommandListResponseBean response = navigate(
                    new String[]{"1", "action 1", "0156fa3e-093e-4136-b95c-01b13dae66c6"},
                    CommandListResponseBean.class).bean();
            assertNotNull(response);
        }

        // verify the case storage has been cleared
        assertFalse(caseSearchStorage.isStorageExists(),
                "Case search storage has not been cleared after case claim");
        assertFalse(caseSearchIndexTable.isStorageExists(), "Case Indexes have not been cleared after case claim");
    }

    /**
     * Test case search:
     *   * with auto-launch (skip to default search results)
     *   * with results in storage (so no claim and auto-launch not fired after selection)
     */
    @Test
    public void testCaseClaimWithResultsInStorageWithAutoLaunch() throws Exception {
        String queryFile = "query_responses/case_claim_response_owned.xml";
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(queryFile)) {
            Response<EntityListResponse> response = navigate(new String[]{"2"},
                    EntityListResponse.class);
            EntityListResponse entityResponse = response.bean();
            assertEquals(entityResponse.getEntities().length, 1);

            // Verify the entity and the calculated fields
            EntityBean entity = entityResponse.getEntities()[0];
            assertEquals(entity.getId(), "3512eb7c-7a58-4a95-beda-205eb0d7f163");
        }

        // Verify the results are stored in storage and not cache
        String cacheKey = "caseclaimdomain_caseclaimuser_http://localhost:8000/a/test/phone/search"
                + "/_case_type=case1=case2=case3_include_closed=False_x_commcare_tag_module_name=Search All Cases";
        assertNull(cacheManager.getCache("case_search").get(cacheKey));

        // Verify case search storage exists for our query
        SQLiteDB caseSearchDb = new CaseSearchDB("caseclaimdomain", "caseclaimuser", null);
        String caseSearchTableName = getCaseSearchTableName(cacheKey);
        UserSqlSandbox caseSearchSandbox = new CaseSearchSqlSandbox(caseSearchTableName, caseSearchDb);
        IStorageUtilityIndexed<Case> caseSearchStorage = caseSearchSandbox.getCaseStorage();
        assertTrue(caseSearchStorage.isStorageExists(), "No case storage found for the given case search query");
        FormplayerCaseIndexTable caseSearchIndexTable = getCaseIndexTable(caseSearchSandbox, caseSearchTableName);
        assertTrue(caseSearchIndexTable.isStorageExists(),
                "No case storage found for the given case search query");


        // Select a case that's already in the casedb
        // (so no claim and also no mark/rewind since auto-launch isn't triggered)
        CommandListResponseBean response = navigate(
                new String[]{"2", "3512eb7c-7a58-4a95-beda-205eb0d7f163"},
                CommandListResponseBean.class).bean();
        assertNotNull(response);

        // case storage has not been cleared since mark/rewind was not triggered
        assertTrue(caseSearchStorage.isStorageExists());
        assertTrue(caseSearchIndexTable.isStorageExists());

        // verify that a 2nd query does not include stale data
        queryFile = "query_responses/case_claim_response.xml";
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(queryFile)) {
            Response<EntityListResponse> searchResponse = navigate(new String[]{"2"},
                    EntityListResponse.class);
            EntityListResponse entityResponse = searchResponse.bean();
            assertEquals(entityResponse.getEntities().length, 1);

            // Verify the entity and the calculated fields
            EntityBean entity = entityResponse.getEntities()[0];
            assertEquals(entity.getId(), "0156fa3e-093e-4136-b95c-01b13dae66c6");
        }
    }

    private String getCaseSearchTableName(String cacheKey) {
        return UserSqlSandbox.FORMPLAYER_CASE + "_" + MD5.toHex(
                MD5.hash(cacheKey.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testPurgeForTemporaryDb() throws Exception {
        // populate case search DB
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_claim_response.xml")) {
            Response<EntityListResponse> response = navigate(new String[]{"1", "action 1"},
                    EntityListResponse.class);
        }

        SqlSandboxUtils.purgeTempDb(Instant.now());

        // verify the case storage has been cleared
        String cacheKey = "caseclaimdomain_caseclaimuser_http://localhost:8000/a/test/phone/search"
                + "/_case_type=case1=case2=case3_include_closed=False";
        SQLiteDB caseSearchDb = new CaseSearchDB("caseclaimdomain", "caseclaimuser", null);
        String caseSearchTableName = getCaseSearchTableName(cacheKey);
        UserSqlSandbox caseSearchSandbox = new CaseSearchSqlSandbox(caseSearchTableName, caseSearchDb);
        IStorageUtilityIndexed<Case> caseSearchStorage = caseSearchSandbox.getCaseStorage();
        assertFalse(caseSearchStorage.isStorageExists(), "Case search storage has not been cleared after purge");
        FormplayerCaseIndexTable caseSearchIndexTable = getCaseIndexTable(caseSearchSandbox, caseSearchTableName);
        assertFalse(caseSearchIndexTable.isStorageExists(), "Case Indexes have not been cleared after purge");

    }

    private FormplayerCaseIndexTable getCaseIndexTable(UserSqlSandbox caseSearchSandbox,
            String caseSearchTableName) {
        String caseIndexTableName = "case_search_index_storage_" + caseSearchTableName;
        return new FormplayerCaseIndexTable(
                caseSearchSandbox, caseIndexTableName, caseSearchTableName, false);
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
