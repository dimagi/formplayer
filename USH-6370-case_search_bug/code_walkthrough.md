# USH-6370 Code Walkthrough: Controller to Cache Collision

A step-by-step trace through the code for a case search request with `selections: ["0"]` on a case list with a clickable icon that references `instance('casedb')`.

## 1. Request entry

**`MenuController.java:177-183`**

```java
BaseResponseBean response = runnerService.advanceSessionWithSelections(
        menuSession, selections, queryData, entityScreenContext, formSessionId);
```

`selections = ["0"]` — select the first module (case search).

## 2. Selection processing

**`MenuSessionRunnerService.java:267-286`**

The selection loop processes `"0"`, calls `handleInput` to select the module, then calls `autoAdvanceSession` with `nextInput = ""` (no further selection — we want the list).

## 3. Query screen handling

**`MenuSessionRunnerService.java:405-412`**

`autoAdvanceSession` encounters a `FormplayerQueryScreen`. Since `autoSearch` is enabled, it calls `doQuery` with `skipCache = true` (no replay, no detail selection, offset = 0).

**`MenuSessionRunnerService.java:519-527`**

```java
ExternalDataInstance searchDataInstance = caseSearchHelper.getRemoteDataInstance(
    screen.getQueryDatum().getDataId(),    // "results"
    screen.getQueryDatum().useCaseTemplate(), // true
    screen.getBaseUrl(), queryParams, skipCache);
screen.updateSession(searchDataInstance);
```

This fetches results from HQ, parses into SQLite, and commits the data instance into the session.

## 4. Case search results → CaseInstanceTreeElement

**`CaseSearchHelper.java:95-125`**

```java
CaseSearchDB caseSearchDb = initCaseSearchDB();
String caseSearchTableName = evalCaseSearchTableName(cacheKey);  // "CCCase_d388..."
CaseSearchSqlSandbox caseSearchSandbox = new CaseSearchSqlSandbox(caseSearchTableName, caseSearchDb);
IStorageUtilityIndexed<Case> caseSearchStorage = caseSearchSandbox.getCaseStorage();
// ... fetch from HQ, parse into caseSearchStorage ...
return new CaseInstanceTreeElement(instanceBase, caseSearchStorage,
        caseSearchIndexTable, caseSearchTableName);
```

The `results` instance is now backed by a `CaseInstanceTreeElement` wrapping `caseSearchStorage` (SQLite table `CCCase_d388...`).

With the fix, `getStorageCacheName()` returns `"casedb:CCCase_d388..."`.
Before the fix, it returned `"casedb"`.

## 5. Entity screen init — back in autoAdvanceSession

**`MenuSessionRunnerService.java:393`**

```java
nextScreen.init(menuSession.getSessionWrapper());
```

This calls `EntityScreen.init` → `initReferences`.

## 6. EntityScreen.initReferences — nodeset evaluation

**`EntityScreen.java:161-172`**

```java
this.setSession(session);
// evalContext now has both instance('casedb') and instance('results')

references = expandEntityReferenceSet(evalContext);
// Evaluates: instance('results')/results/case[@case_type='capacity'][not(commcare_is_related_case=true())]
// Returns ~184 TreeReferences into the results instance

QueryContext newContext = evalContext.getCurrentQueryContext()
        .checkForDerivativeContextAndReturn(references.size());
// If references.size() > 50 and > 10x parent scope → new child QueryContext
evalContext.setQueryContext(newContext);
```

**This is the first QueryContext escalation.** The new context is a child of the original, linked via `QueryCacheHost` parent chain.

## 7. Entity list construction — detail field evaluation

**`EntityScreen.java:139-143` → `EntityListSubscreen` constructor → `EntityScreenHelper.initEntities`**

For each entity reference, `NodeEntityFactory.getEntity` evaluates every detail field:

**`NodeEntityFactory.java:66`**

```java
fieldData[count] = f.getTemplate().evaluate(nodeContext);
```

For most fields this is simple — `@case_type`, `case_name`, etc., evaluated against the case search results instance. No collision.

## 8. The clickable icon field fires

One detail field has this XPath template:

```xpath
if(selected(
  instance('casedb')/casedb/case[
    @case_id = instance('casedb')/casedb/case[
      @case_type = 'commcare-user'
    ][hq_user_id = instance('commcaresession')/session/context/userid]/@case_id
  ]/favorite_clinic_case_ids,
  current()/index/parent),
'yes', 'no')
```

This references `instance('casedb')` — the **user's local case database**, not the search results. The XPath engine resolves this against the `casedb` `CaseInstanceTreeElement` in `evalContext.formInstances`.

## 9. Predicate evaluation triggers batch fetch on casedb

**`StorageBackedTreeRoot.java:67-107` — `tryBatchChildFetch`**

The predicate `[@case_id = ...]` on `instance('casedb')` triggers the query planner:

```java
queryContext = evalContext.getCurrentQueryContext();
// This is the ESCALATED context from step 6 (scope = references.size())
```

**`StorageBackedTreeRoot.java:356-359` — `reportBulkRecordSet`**

```java
if (ids.size() > 50 && ...) {
    RecordSetResultCache cue = currentQueryContext.getQueryCache(RecordSetResultCache.class);
    cue.reportBulkRecordSet(cacheKey, getStorageCacheName(), ids);
    //                                 ^^^^^^^^^^^^^^^^
    //                     Returns "casedb" for the user's casedb instance
}
```

The user's casedb record IDs are registered in the `RecordSetResultCache` under key `"casedb"`.

## 10. QueryContext parent chain shares the cache

**`QueryCacheHost.java:40-54` — `getQueryCache`**

```java
public <T extends QueryCache> T getQueryCache(Class<T> cacheType) {
    T existing = getQueryCacheOrNull(cacheType);  // walks parent chain
    if (existing != null) {
        return existing;  // returns the SAME cache from any ancestor
    }
    // ... create new if not found
}
```

**`QueryCacheHost.java:60-68` — `getQueryCacheOrNull`**

```java
if (cacheEntries.containsKey(cacheType)) {
    return cacheEntries.get(cacheType);
}
if (parent != null) {
    return parent.getQueryCacheOrNull(cacheType);  // walk up
}
```

The `RecordObjectCache` and `RecordSetResultCache` created during step 6's context escalation are visible to ALL child contexts via parent chain walking. Both the casedb and results tree elements share the same cache instances.

## 11. The collision — casedb records served for results lookups

**`StorageInstanceTreeElement.java:295-312` — `getElement`**

When a `CaseChildElement` from the **results** instance needs to load its Case data:

```java
String storageCacheKey = getStorageCacheName();
// Before fix: "casedb" (SAME as user's casedb)
// After fix:  "casedb:CCCase_d388..." (UNIQUE)

RecordObjectCache recordObjectCache = getRecordObjectCacheIfRelevant(context);

if (recordObjectCache.isLoaded(storageCacheKey, recordId)) {
    return recordObjectCache.getLoadedRecordObject(storageCacheKey, recordId);
    //     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //     Before fix: returns a Case from the USER'S CASEDB table
    //     (loaded in step 9) instead of from the case search table.
    //     The recordId (e.g., 42) exists in BOTH tables but refers
    //     to DIFFERENT cases.
}
```

**`RecordObjectCache.java:18-30`**

```java
HashMap<String, HashMap<Integer, T>> caches;
//      ^^^^^^^                       ^
//      "casedb"                      recordId → Case object
//
// Both instances write/read the SAME inner HashMap
// because they use the same storageCacheKey "casedb"
```

## 12. Wrong data reaches the entity list

The `CaseChildElement.cache()` method stores the wrong `Case` into the per-instance `treeCache`:

**`CaseChildElement.java:100-104`**

```java
Case c = parent.getElement(recordId, context);
// c is from the user's casedb (wrong!) instead of the search table
entityId = c.getCaseId();
return buildAndCacheInternalTree(c);
// Stores a TreeElement with wrong case_type, case_name, etc.
// into parent.treeCache — all future reads on this instance
// return the wrong data (even with null context).
```

The entity list now contains cases with types like `unit`, `clinic`, `provider` instead of all `capacity`.

## Why it's intermittent

The `RecordObjectCache` is only created when the QueryContext scope exceeds `BULK_QUERY_THRESHOLD` (50):

**`StorageInstanceTreeElement.java:338-345`**

```java
if (context.getScope() < QueryContext.BULK_QUERY_THRESHOLD) {
    return context.getQueryCacheOrNull(RecordObjectCache.class);
    // Returns null if no cache exists yet → no collision possible
} else {
    return context.getQueryCache(RecordObjectCache.class);
    // Creates the cache if needed → collision possible
}
```

Whether the scope escalates depends on `QueryContext.dominates()`:

**`QueryContext.java:121-125`**

```java
private boolean dominates(int existingScope, int newScope) {
    return newScope > existingScope
        && newScope > BULK_QUERY_THRESHOLD
        && newScope / existingScope > 10;
}
```

The escalation depends on the ratio of result set sizes between nested evaluations. Different evaluation orderings (driven by predicate optimization, expression caching, and the number of entities) produce different scope chains. Some chains escalate past the threshold and create the cache; others don't.

## The fix

**`CaseInstanceTreeElement.java:81-88`** — new 4-arg constructor:

```java
public CaseInstanceTreeElement(AbstractTreeElement instanceRoot,
                               IStorageUtilityIndexed<Case> storage,
                               CaseIndexTable caseIndexTable,
                               @Nullable String storageCacheIdentifier) {
    super(instanceRoot, storage, MODEL_NAME, "case");
    this.caseIndexTable = caseIndexTable;
    this.storageCacheIdentifier = storageCacheIdentifier;
}
```

**`CaseInstanceTreeElement.java:161-166`** — unique cache name:

```java
public String getStorageCacheName() {
    if (storageCacheIdentifier != null) {
        return MODEL_NAME + ":" + storageCacheIdentifier;
    }
    return MODEL_NAME;
}
```

**`CaseSearchHelper.java:125`** — passes table name:

```java
return new CaseInstanceTreeElement(instanceBase, caseSearchStorage,
        caseSearchIndexTable, caseSearchTableName);
```

User's casedb caches under `"casedb"`. Case search results cache under `"casedb:CCCase_d388..."`. Same `RecordObjectCache`, different keys, no collision.
