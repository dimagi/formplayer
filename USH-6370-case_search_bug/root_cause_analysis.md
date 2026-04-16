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

### 4. Intermittency explained

The collision only occurs when both instances share the same `QueryContext` (and thus the same `RecordObjectCache`). Whether this happens depends on how the evaluation context forks query contexts during nodeset evaluation — which varies by execution path and request state. This is why identical requests from the test script produce different outcomes.

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
