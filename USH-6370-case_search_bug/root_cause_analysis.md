# USH-6370 Root Cause: RecordObjectCache key collision

## Summary

When case search results are displayed via an entity list, the `results` data instance (case search) and the `casedb` data instance (user's full case database) are both backed by `CaseInstanceTreeElement`. Both return `"casedb"` from `getStorageCacheName()`, which is used as the key prefix in `RecordObjectCache`. When both instances share the same `QueryContext` during nodeset evaluation, their record lookups collide — one instance reads the other's data from the cache.

## Affected code

- `CaseInstanceTreeElement.getStorageCacheName()` — always returns `"casedb"` (hard-coded `MODEL_NAME`)
- `StorageInstanceTreeElement.getElement(int recordId, QueryContext context)` — uses `getStorageCacheName() + recordId` as cache key in `RecordObjectCache`
- `RecordObjectCache` — per-`QueryContext` cache, shared when two instances are evaluated in the same context scope

## Evidence

### 1. Storage is clean, tree reads are not

Across all failure runs, `getExternalRoot` iterates the SQLite storage directly (`storage.iterate()`) and reports all cases as `capacity`. But the inventory (which reads via `CaseChildElement.cache()` → `getElement()` → `RecordObjectCache`) reports mixed types from the same instance in the same request:

| Checkpoint | Path | Result |
|---|---|---|
| `getExternalRoot` (SqlStorage.iterate) | Direct SQL read | `storageCount=1189 caseTypes=[capacity]` |
| Inventory (CaseChildElement.cache) | Via RecordObjectCache | `{capacity=1047, clinic=2, commcare-case-claim=138, provider=1, unit=1}` |

Same backing storage, same request, same count (1189) — different case types. The divergence is in the cache layer.

### 2. Both instances share the cache key `"casedb"`

Inventory confirmed on every run:

```
casedb  ... storageCacheName=casedb
results ... storageCacheName=casedb
```

### 3. Cross-instance cache read on failure, separate caches on success

`RecordObjectCache` bulk-load and hit events captured via `ThreadLocal` diagnostic log:

**Failure run** — two tree elements share `cache=1061465914`:

```
BULK_LOAD key=casedb te=1529968747 storage=1095096645 cache=1061465914 count=1189
HIT       key=casedb te=1808671756 storage=1945215430 cache=1061465914 recordId=313
```

`te=1529968747` (one storage) populated the cache. `te=1808671756` (different storage) read from it. **Cross-instance read confirmed.**

**Success run** — each tree element gets its own cache:

```
BULK_LOAD key=casedb te=1107500281 cache=1691588858 count=1189
BULK_LOAD key=casedb te=732472401  cache=2112698688 count=1189
```

Different `cache=` hashes. No cross-instance events.

### 4. Why clickable icons trigger the collision

The bug reproduces on case lists with "clickable icons" — detail fields whose XPath cross-references `instance('casedb')`. The test app's clickable icon field uses:

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

This expression queries `instance('casedb')` during entity detail field evaluation. During `NodeEntityFactory.getEntity()`, each entity's detail field template is evaluated via `f.getTemplate().evaluate(nodeContext)`. This XPath hits the user's `CaseInstanceTreeElement`, triggering `tryBatchChildFetch` → `reportBulkRecordSet` with key `"casedb"`. The user's casedb records get bulk-loaded into the `RecordObjectCache` under key `"casedb"`.

When the case search `results` instance later does its own bulk load, it uses the same key `"casedb"` in the same `RecordObjectCache` (shared via `QueryContext` parent chain — `QueryCacheHost.getQueryCacheOrNull` walks up parents at `QueryCacheHost.java:60-63`). Records from the user's casedb overwrite or are read in place of records from the search table.

Apps without `instance('casedb')` references in their detail fields never load the user's casedb during entity evaluation, so the collision never fires.

### 5. Intermittency

The collision mechanism is confirmed by the diagnostic logs: in failure runs, two tree elements with different storage objects share a single `RecordObjectCache` instance and one reads the other's data. In success runs, each tree element gets its own `RecordObjectCache` instance — no cross-instance reads.

The exact trigger for this difference is **not fully explained**. The cache is per-request, request handling is single-threaded, and the inputs are identical between iterations. The `RecordObjectCache` topology (shared vs separate) should therefore be deterministic — yet it isn't.

Evidence that the inputs are the same:
- Both sentry_8_mismatch and sentry_8_ok show `casedb children=455` and `results children=1189`
- Same table name, same cache key hash, same storage counts

Candidates for the remaining non-determinism:
- **QueryContext forking differences.** The `RecordObjectCache` is obtained via `QueryCacheHost.getQueryCacheOrNull`, which walks a parent chain. Whether two tree elements end up in the same or different `QueryContext` branch depends on scope escalation decisions in the evaluation engine. A subtle difference in evaluation order could produce a different `QueryContext` topology.
- **State from prior iterations.** The test script clicks a random case between list requests. If this triggers a case claim, the casedb grows by one case per iteration. We observed casedb growing from 389 to 455 across test runs. Within a single run the growth could shift record ID distributions and affect query planner behavior.
- **Something not yet identified.** A debugger session stepping through `QueryContext.checkForDerivativeContextAndReturn` and `QueryCacheHost.getQueryCache` on both a success and failure iteration would pin down the exact divergence point.

The fix is correct regardless of the intermittency trigger: unique cache keys make cross-instance reads impossible no matter what the `QueryContext` topology looks like.

## Fix

`CaseInstanceTreeElement.getStorageCacheName()` must return a key unique per storage instance, not the generic `"casedb"`. The underlying SqlStorage table name is the natural choice — each case search query gets its own table, and the user's casedb has its own.

Example:
```java
// Before (collision):
public String getStorageCacheName() {
    return CaseInstanceTreeElement.MODEL_NAME;  // always "casedb"
}

// After (unique per storage):
public String getStorageCacheName() {
    return CaseInstanceTreeElement.MODEL_NAME + ":" + storage.getStorageIdentifier();
}
```

Where `getStorageIdentifier()` returns the table name for SQL-backed storage.
