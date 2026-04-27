package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMultimap;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.instance.StorageInstanceTreeElement;
import org.commcare.cases.model.Case;
import org.commcare.cases.query.QueryContext;
import org.commcare.modern.engine.cases.RecordObjectCache;
import org.commcare.formplayer.application.MenuController;
import org.commcare.formplayer.services.CaseSearchHelper;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.beans.menus.BaseResponseBean;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.database.models.FormplayerCaseIndexTable;
import org.commcare.formplayer.junit.InitializeStaticsExtension;
import org.commcare.formplayer.junit.Installer;
import org.commcare.formplayer.junit.RestoreFactoryAnswer;
import org.commcare.formplayer.junit.RestoreFactoryExtension;
import org.commcare.formplayer.junit.StorageFactoryExtension;
import org.commcare.formplayer.junit.request.Response;
import org.commcare.formplayer.junit.request.SessionNavigationRequest;
import org.commcare.formplayer.mocks.FormPlayerPropertyManagerMock;
import org.commcare.formplayer.utils.HqUserDetails;
import org.commcare.formplayer.utils.WithHqUserSecurityContextFactory;
import org.commcare.formplayer.sandbox.CaseSearchSqlSandbox;
import org.commcare.formplayer.sandbox.SqlSandboxUtils;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.services.FormplayerStorageFactory;
import org.commcare.formplayer.services.InstallService;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.sqlitedb.CaseSearchDB;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.commcare.formplayer.utils.MockRequestUtils;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.web.client.WebClient;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.MD5;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    @Autowired
    CaseSearchHelper caseSearchHelper;

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
        FormPlayerPropertyManagerMock.mockIndexCaseSearchResults(storageFactoryMock, true);
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
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

    /**
     * Verify that case search results backed by SQLite storage produce a
     * CaseInstanceTreeElement with a unique storageCacheName. Without this,
     * the user's casedb and the search results share RecordObjectCache entries
     * under the same "casedb" key, causing cross-instance data pollution (USH-6370).
     */
    @Test
    public void testCaseSearchResultsHaveUniqueStorageCacheNameWithFfOn() throws Exception {
        WithHqUserSecurityContextFactory.setSecurityContext(
                HqUserDetails.builder().enabledToggles(new String[]{"CASE_SEARCH_CACHE_KEY"}).build()
        );

        ImmutableMultimap<String, String> requestData = ImmutableMultimap.of(
                "case_type", "case1",
                "case_type", "case2",
                "case_type", "case3",
                "include_closed", "False");

        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_claim_response.xml")) {
            var instance = caseSearchHelper.getRemoteDataInstance(
                    "results", true,
                    new java.net.URL("http://localhost:8000/a/test/phone/search/"),
                    requestData, false);

            var root = instance.getRoot();
            assertTrue(root instanceof CaseInstanceTreeElement,
                    "Expected CaseInstanceTreeElement for indexed case search results");

            String cacheName = ((CaseInstanceTreeElement) root).getStorageCacheName();
            assertNotEquals(CaseInstanceTreeElement.MODEL_NAME, cacheName,
                    "Case search results must not use plain 'casedb' as storage cache name");
            assertTrue(cacheName.startsWith(CaseInstanceTreeElement.MODEL_NAME + ":"),
                    "Storage cache name should be prefixed with 'casedb:'");
        }
    }

    @Test
    public void testCaseSearchResultsStorageCacheNameClashes() throws Exception {

        ImmutableMultimap<String, String> requestData = ImmutableMultimap.of(
                "case_type", "case1",
                "case_type", "case2",
                "case_type", "case3",
                "include_closed", "False");

        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_claim_response.xml")) {
            var instance = caseSearchHelper.getRemoteDataInstance(
                    "results", true,
                    new java.net.URL("http://localhost:8000/a/test/phone/search/"),
                    requestData, false);

            var root = instance.getRoot();
            assertTrue(root instanceof CaseInstanceTreeElement,
                    "Expected CaseInstanceTreeElement for indexed case search results");

            String cacheName = ((CaseInstanceTreeElement) root).getStorageCacheName();
            assertEquals(CaseInstanceTreeElement.MODEL_NAME, cacheName,
                    "Case search results should use 'casedb' as storage cache name without FF");
        }
    }



    /**
     * Functional regression test for USH-6370.
     *
     * Navigates a case list that uses BOTH case search indexing (storage-instance="results") AND
     * a detail field that cross-references instance('casedb'). Without the fix, the shared
     * RecordObjectCache key "casedb" causes the search results instance to read from the user's
     * casedb instead of the search table, corrupting the entity display data. With the fix,
     * unique cache keys per storage table prevent any cross-instance contamination.
     *
     * Verifies that:
     * - entity data from instance('results') is correct (would be "" without fix)
     * - entity data from the instance('casedb') cross-reference field is also correct
     */
    @Test
    public void testCaseListWithCasedbCrossReferenceInSearchDetail() throws Exception {
        WithHqUserSecurityContextFactory.setSecurityContext(
                HqUserDetails.builder().enabledToggles(new String[]{"CASE_SEARCH_CACHE_KEY"}).build()
        );

        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_claim_response.xml")) {
            Response<EntityListResponse> response = navigate(new String[]{"1", "action 1"},
                    EntityListResponse.class);
            EntityListResponse entityResponse = response.bean();
            assertEquals(1, entityResponse.getEntities().length);

            EntityBean entity = entityResponse.getEntities()[0];
            assertEquals("0156fa3e-093e-4136-b95c-01b13dae66c6", entity.getId());

            // Field 0: case_name from the search results storage entity
            assertEquals("Burt Maclin", entity.getData()[0]);

            // Field 1: instance('results') cross-lookup — would return "" on cache collision
            assertEquals("Burt Maclin", entity.getData()[1],
                    "instance('results') lookup corrupted: RecordObjectCache key collision with casedb");

            // Field 2: parent case name from instance('results') — would return "" on cache collision
            assertEquals("Kurt Maclin", entity.getData()[2],
                    "instance('results') parent lookup corrupted: RecordObjectCache key collision with casedb");

            // Field 3: count from instance('casedb') — triggers the casedb bulk-load that causes
            // the collision when the cache key is not unique. The restore has 7 open 'case'-type cases.
            assertEquals("7", entity.getData()[3],
                    "instance('casedb') count field returned wrong value");
        }
    }

    /**
     * Gap 2: Verifies the full case claim flow (entity list → case selection → claim POST → sync →
     * storage cleared) with the CASE_SEARCH_CACHE_KEY FF enabled. Ensures the unique cache key
     * doesn't break any step of the claim workflow.
     */
    @Test
    public void testCaseClaimFlowWithFfOn() throws Exception {
        WithHqUserSecurityContextFactory.setSecurityContext(
                HqUserDetails.builder().enabledToggles(new String[]{"CASE_SEARCH_CACHE_KEY"}).build()
        );

        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_claim_response.xml")) {
            Response<EntityListResponse> response = navigate(new String[]{"1", "action 1"},
                    EntityListResponse.class);
            EntityListResponse entityResponse = response.bean();
            assertEquals(1, entityResponse.getEntities().length);

            EntityBean entity = entityResponse.getEntities()[0];
            assertEquals("0156fa3e-093e-4136-b95c-01b13dae66c6", entity.getId());
            assertEquals("Burt Maclin", entity.getData()[0]);
            assertEquals("Burt Maclin", entity.getData()[1]);
            assertEquals("Kurt Maclin", entity.getData()[2]);
            assertEquals("7", entity.getData()[3]);
        }

        // Verify results are in storage (not in-memory cache)
        String cacheKey = "caseclaimdomain_caseclaimuser_http://localhost:8000/a/test/phone/search"
                + "/_case_type=case1=case2=case3_include_closed=False_x_commcare_tag_module_name=Search All Cases";
        assertNull(cacheManager.getCache("case_search").get(cacheKey));

        SQLiteDB caseSearchDb = new CaseSearchDB("caseclaimdomain", "caseclaimuser", null);
        String caseSearchTableName = getCaseSearchTableName(cacheKey);
        UserSqlSandbox caseSearchSandbox = new CaseSearchSqlSandbox(caseSearchTableName, caseSearchDb);
        IStorageUtilityIndexed<Case> caseSearchStorage = caseSearchSandbox.getCaseStorage();
        assertTrue(caseSearchStorage.isStorageExists());
        FormplayerCaseIndexTable caseSearchIndexTable = getCaseIndexTable(caseSearchSandbox, caseSearchTableName);
        assertTrue(caseSearchIndexTable.isStorageExists());

        // Case selection → claim → sync
        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockPost(true)) {
            RestoreFactoryAnswer answer = new RestoreFactoryAnswer("restores/caseclaim2.xml");
            Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml();

            CommandListResponseBean claimResponse = navigate(
                    new String[]{"1", "action 1", "0156fa3e-093e-4136-b95c-01b13dae66c6"},
                    CommandListResponseBean.class).bean();
            assertNotNull(claimResponse);
        }

        // Storage cleared after claim
        assertFalse(caseSearchStorage.isStorageExists());
        assertFalse(caseSearchIndexTable.isStorageExists());
    }

    /**
     * Gap 3: Deterministic regression guard for the RecordObjectCache cache key collision (USH-6370).
     *
     * The intermittent collision requires two CaseInstanceTreeElements to share the same
     * RecordObjectCache (via a shared QueryContext). This test forces that scenario directly:
     * it pre-poisons RecordObjectCache["casedb"] with a wrong Case at a known record ID, then
     * calls getElement() on the search results element. With the fix (unique cache key), the
     * element checks its own key ("casedb:<hash>") — a cache miss — and reads the correct case
     * from storage. Without the fix, it would check "casedb", get a cache hit, and return the
     * wrong case.
     *
     * This test fails deterministically if the unique cache key logic is reverted.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testUniqueStorageKeyPreventsRecordObjectCacheCollision() throws Exception {
        WithHqUserSecurityContextFactory.setSecurityContext(
                HqUserDetails.builder().enabledToggles(new String[]{"CASE_SEARCH_CACHE_KEY"}).build()
        );

        ImmutableMultimap<String, String> requestData = ImmutableMultimap.of(
                "case_type", "case1",
                "case_type", "case2",
                "case_type", "case3",
                "include_closed", "False");

        try (MockRequestUtils.VerifiedMock ignore = mockRequest.mockQuery(
                "query_responses/case_claim_response.xml")) {
            var instance = caseSearchHelper.getRemoteDataInstance(
                    "results", true,
                    new java.net.URL("http://localhost:8000/a/test/phone/search/"),
                    requestData, false);

            CaseInstanceTreeElement searchElement = (CaseInstanceTreeElement) instance.getRoot();
            String uniqueKey = searchElement.getStorageCacheName();
            assertNotEquals(CaseInstanceTreeElement.MODEL_NAME, uniqueKey,
                    "Pre-condition: fix must be active (unique cache key)");

            // Access the storage backing the search results element to find a real record ID.
            Field storageField = StorageInstanceTreeElement.class.getDeclaredField("storage");
            storageField.setAccessible(true);
            IStorageUtilityIndexed<Case> searchStorage =
                    (IStorageUtilityIndexed<Case>) storageField.get(searchElement);

            var iter = searchStorage.iterate();
            assertTrue(iter.hasMore(), "Search results storage must have at least one case");
            Case expectedCase = iter.nextRecord();
            int recordId = expectedCase.getID();
            assertEquals("Burt Maclin", expectedCase.getName());

            // Build a shared QueryContext and pre-register a RecordObjectCache so it will be used
            // by getElement() even at low query scope (simulating a shared QueryContext scenario).
            QueryContext sharedContext = new QueryContext();
            RecordObjectCache<Case> sharedCache = sharedContext.getQueryCache(RecordObjectCache.class);

            // Poison the plain "casedb" slot — this is what the user's casedb would load when
            // a detail field references instance('casedb') during entity evaluation.
            // We use the second case in search results as the "wrong" case so it's a real
            // Case object but with a different caseId than expectedCase.
            assertTrue(iter.hasMore(), "Need at least 2 cases in search results for collision test");
            Case wrongCase = iter.nextRecord();
            assertNotEquals(expectedCase.getCaseId(), wrongCase.getCaseId(),
                    "Pre-condition: poisoned case must differ from expected search result");
            sharedCache.getLoadedCaseMap(CaseInstanceTreeElement.MODEL_NAME).put(recordId, wrongCase);

            // Call getElement() on the search results element with the poisoned shared context.
            // With fix: checks "casedb:<hash>" → cache miss → reads from storage → correct case.
            // Without fix: checks "casedb" → cache hit with wrong case → wrong case returned.
            Method getElementMethod = StorageInstanceTreeElement.class.getDeclaredMethod(
                    "getElement", int.class, QueryContext.class);
            getElementMethod.setAccessible(true);
            Case result = (Case) getElementMethod.invoke(searchElement, recordId, sharedContext);

            assertNotNull(result);
            assertEquals(expectedCase.getCaseId(), result.getCaseId(),
                    "Search results element returned wrong case: unique cache key did not isolate "
                            + "it from poisoned 'casedb' slot in shared RecordObjectCache");
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
