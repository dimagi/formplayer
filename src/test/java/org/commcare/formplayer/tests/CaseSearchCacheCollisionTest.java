package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.model.Case;
import org.commcare.cases.query.QueryContext;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.modern.engine.cases.RecordObjectCache;
import org.javarosa.core.model.instance.InstanceBase;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;

/**
 * Reproduces USH-6370: CaseInstanceTreeElement.getStorageCacheName() returns "casedb"
 * for ALL instances, causing RecordObjectCache collisions between the user's casedb
 * and case search results when they share a QueryContext.
 */
public class CaseSearchCacheCollisionTest {

    private static final String DB_FILE = "test_cache_collision.db";
    private Connection connection;

    @BeforeEach
    public void setUp() throws Exception {
        new File(DB_FILE).delete();
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        new File(DB_FILE).delete();
    }

    /**
     * Demonstrates that two CaseInstanceTreeElement instances — one for the user's
     * casedb and one for case search results — return the same getStorageCacheName().
     * This shared cache key is the root cause of USH-6370.
     */
    @Test
    public void testStorageCacheNameCollision() {
        SqlStorage<Case> casedbStorage = new SqlStorage<>(
                () -> connection, Case.class, "user_casedb");
        SqlStorage<Case> searchResultsStorage = new SqlStorage<>(
                () -> connection, Case.class, "search_results");

        CaseInstanceTreeElement casedbInstance = new CaseInstanceTreeElement(
                new InstanceBase("casedb"), casedbStorage);
        CaseInstanceTreeElement resultsInstance = new CaseInstanceTreeElement(
                new InstanceBase("results"), searchResultsStorage);

        // Both instances return "casedb" regardless of which storage they wrap
        assertEquals(casedbInstance.getStorageCacheName(), resultsInstance.getStorageCacheName(),
                "Both instances return the same storageCacheName, causing cache collisions");
    }

    /**
     * Demonstrates the full collision: when casedb records are bulk-loaded into the
     * shared RecordObjectCache, a subsequent lookup for a search-results record with
     * the same SQLite row ID returns the casedb record instead.
     *
     * In production this manifests as case search results showing data (case_type,
     * case_name, etc.) from the user's casedb instead of from the search results.
     */
    @Test
    public void testRecordObjectCacheCrossContamination() {
        // Two CaseInstanceTreeElement instances backed by different storages
        IStorageUtilityIndexed<Case> casedbStorage = new SqlStorage<>(
                () -> connection, Case.class, "user_casedb");
        IStorageUtilityIndexed<Case> searchResultsStorage = new SqlStorage<>(
                () -> connection, Case.class, "search_results");

        CaseInstanceTreeElement casedbInstance = new CaseInstanceTreeElement(
                new InstanceBase("casedb"), casedbStorage);
        CaseInstanceTreeElement resultsInstance = new CaseInstanceTreeElement(
                new InstanceBase("results"), searchResultsStorage);

        // Create a shared QueryContext with scope above BULK_QUERY_THRESHOLD
        // (in production, both instances share the EvaluationContext's QueryContext)
        QueryContext context = new QueryContext();
        context = context.checkForDerivativeContextAndReturn(100);

        // Simulate bulk loading from the casedb instance: a Case of type "person"
        // gets loaded into the RecordObjectCache under key ("casedb", 1)
        Case casedbCase = new Case("Alice", "person");
        casedbCase.setCaseId("casedb-case-1");
        casedbCase.setID(1);

        RecordObjectCache<Case> recordCache = context.getQueryCache(RecordObjectCache.class);
        String casedbCacheName = casedbInstance.getStorageCacheName();
        recordCache.getLoadedCaseMap(casedbCacheName).put(1, casedbCase);

        // Now the search results instance has a different Case at record ID 1,
        // type "facility" — but it will never see it through the cache
        String resultsCacheName = resultsInstance.getStorageCacheName();

        // The cache keys are identical — this is the bug
        assertEquals("casedb", casedbCacheName);
        assertEquals("casedb", resultsCacheName);

        // The results instance asks for record ID 1 and gets the casedb's Case
        assertTrue(recordCache.isLoaded(resultsCacheName, 1),
                "Cache reports record 1 as loaded for the results instance, "
                        + "even though it was loaded by the casedb instance");

        Case retrieved = recordCache.getLoadedRecordObject(resultsCacheName, 1);
        assertEquals("person", retrieved.getTypeId(),
                "Retrieved case has type 'person' (from casedb), not 'facility' (from search results)");
        assertEquals("Alice", retrieved.getName(),
                "Retrieved case has name 'Alice' (from casedb), not the search result's name");
    }
}
