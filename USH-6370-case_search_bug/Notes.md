## issue
When using case search with grouped cases formplayer sometimes includes child cases of the wrong type. All cases should have the same case type.

This does not happen very often but we have  script to reproduce it: `test-case-search_py.py`

## logging

### production

On the formplayer side we added logging that is specific to the BHA where we have seen the issue. It depends on the first column to be empty when the cases are not of the correct type. That change is in master in the file: `/src/main/java/org/commcare/formplayer/application/MenuController.java#L216`

### staging

The branch `riese/skip_cache_2` has more logging and has been deployed to staging. All the added logs start with the prefix "USH-6370"

## Things to investigate

- [ ] Does the logging in CaseSearchHelper rule out the issue being in the hq case search?
- [ ] Make a table of logging locations and if we see the issue there.
- [ ] How do the wrongly returned cases related to the parent? Are they actual children? Just of the wrong type?

## Checkpoint table

Tracks where the wrong-case-type issue is visible as the response flows
through formplayer. "ok" = only the expected case type seen. "mismatch" =
multiple case types observed.

| # | Checkpoint                                  | File:Line                                    | What it checks                                                 | Result   | Count |
|---|---------------------------------------------|----------------------------------------------|----------------------------------------------------------------|----------|-------|
| 1 | `validateCaseTypesInResponse`               | CaseSearchHelper.java:175                    | Raw HQ XML response bytes                                      | ok       | 184 cases |
| 2 | `validateCaseTypesInStorage`                | CaseSearchHelper.java:219                    | SQLite storage after parsing                                   | ok       | — |
| 3 | `initReferences` (commcare-core)            | EntityScreen.initReferences                  | Case IDs picked up by the nodeset evaluation                   | oversized | 238 IDs (54 not in HQ response) |
| 4 | `entityListScreen.getEntities` (sb1)        | EntityListResponse.java ~L91                 | Full entity list before pagination                             | mismatch | 5 types: unit, commcare-case-claim, provider, clinic, capacity |
| 5 | `paginateEntities` (sb2)                    | EntityListResponse.java ~L102                | Paginated page of entities                                     | mismatch | 2 types: unit, capacity |
| 6 | `processEntitiesForCaseList` (sb3)          | EntityListResponse.java ~L112                | Final `EntityBean[]` after bean conversion                     | mismatch | 2 types: unit, capacity (106 IDs, only 13 in HQ) |
| 7 | `controller` (`logCaseTypeColumnIfPresent`) | MenuController.java:225                      | Final `EntityListResponse` returned from controller            | mismatch | 2 types: unit, capacity |

### Key findings from sentry_logs_2

1. **HQ → SQLite is clean.** Checkpoints 1–2 rule out HQ and parsing.
2. **`initReferences` already has 54 extra IDs** not present in the HQ
   response. The nodeset is evaluating against something larger than the
   search-results instance (or against the search-results instance
   after it's been polluted).
3. **Of the 106 cases shown to the user, only 13 came from the HQ
   search response.** 93 are from elsewhere. So the entity list is
   overwhelmingly populated with cases outside the search results.
4. **Pagination/bean conversion don't change the set** — they just
   narrow/transform it. Mismatch at checkpoints 5/6/7 is downstream
   of the root cause.
5. Checkpoint 6's prior "ok" reading was **invalid** — it only detected
   `GraphData` → HTML transforms via `processData()`, never case-type
   changes. Fixed in commit `cc98d17bc`.

### Hypothesis

The `EntityScreen`'s evaluation context references an `ExternalDataInstance`
whose contents include more than the current request's case search
results — either a stale/cached instance from a prior request, or the
data instance is being merged with the regular casedb. Worth checking:

- The `ExternalDataInstance` identity/source bound to the screen at init
  time vs. what was just written to SQLite for this request.
- `entityScreenCache` in `MenuSession` — does a reused cached screen
  carry a stale data instance reference?
- Whether `FormplayerRemoteInstanceFetcher` returns the fresh instance
  even when `skipCache=true` is in effect.

### Findings from sentry_4 (EvalContext inventory, after logging fix)

On a confirmed failure iteration, the `results` `DataInstance` bound to
the `EntityScreen` holds **1189 cases of 4 types** (`capacity`, `unit`,
`provider`, `clinic`) — even though HQ returned only 184 capacity cases
and `validateCaseTypesInStorage` said "ok" earlier in the same request.

| Instance              | class            | ref                                | rootName | children | caseTypes (sampled)                      |
|-----------------------|------------------|------------------------------------|----------|----------|------------------------------------------|
| `casedb`              | CaseDataInstance | jr://instance/casedb               | casedb   | 380      | [commcare-case-claim] (5)                |
| **`results`**         | CaseDataInstance | jr://instance/remote/results       | casedb   | **1189** | **[capacity, unit, provider, clinic]** (50) |
| `search-input:results`| ExternalDataInstance | jr://instance/search-input/results | input  | 0        | —                                        |
| `commcaresession`     | ExternalDataInstance | jr://instance/session          | session  | 3        | —                                        |

Source URI on `results` is correct (points at the case search endpoint
for this app). Both `casedb` and `results` are backed by
`CaseInstanceTreeElement` (SQLite). So the `results` instance is wired
to a SQLite table that contains 1189 cases from multiple case types,
not just the 184 capacity cases this request wrote and validated.

### New hypothesis (post sentry_4)

Between `validateCaseTypesInStorage` (run right after
`parseIntoCaseSearchStorage` writes the HQ response) and the moment the
`CaseInstanceTreeElement` is iterated by the entity screen, the SQLite
table backing the `results` instance has accumulated extra rows.

Likely mechanisms:
1. **Concurrent request pollution** — multiple requests share the same
   `formplayer_case_<hash>` table and race: A's `removeAll()` +
   `parseIntoSandbox` overlaps with B's, or A reads after B has written
   different data into the same table.
2. **Cache-key collision** — two logically-different searches produce
   the same `cacheKey` → same `caseSearchTableName`. Different search
   queries (different case_types) would pile up in one table.
3. **`removeAll()` not clearing** — the per-table `removeAll()` leaves
   rows, so each request adds to a growing set.

1189 cases / 184 per request ≈ 6 requests of accumulated data, which is
consistent with either mechanism.

### Added logging (commit TBD)

`CaseSearchHelper.getExternalRoot` now logs just before returning the
`CaseInstanceTreeElement` to the caller:

```
USH-6370 getExternalRoot returning tree: table=<hash> cacheKeyHash=<hex>
storageCount=<N> caseTypes=[...]
```

- If `storageCount` != the count validated a few lines earlier in the
  same call, the write/read gap is where the pollution enters.
- If two requests log the **same** `table` but **different** `cacheKeyHash`,
  cache-key collision (mechanism 2) is confirmed.
- If two concurrent requests log the same table + same key but with
  different case counts/types in quick succession, mechanism 1.

## Possible root cause

Walking through the code:

1. CaseChildElement.cache() calls parent.getElement(recordId, context) to get case data (libs/commcare/.../CaseChildElement.java:100)
2. StorageInstanceTreeElement.getElement() checks a RecordObjectCache keyed by getStorageCacheName() + recordId (StorageInstanceTreeElement.java:285-314)
3. CaseInstanceTreeElement.getStorageCacheName() always returns "casedb" (hard-coded to MODEL_NAME) — regardless of which storage the instance wraps (CaseInstanceTreeElement.java:145-147)

The collision: The user's casedb instance AND the results (case-search) instance are BOTH CaseInstanceTreeElement, BOTH return "casedb" from getStorageCacheName(). They share cache keys in
the RecordObjectCache.

What this causes:
- When the real casedb instance bulkReads records (say, recordId=42), the cache stores ("casedb", 42) → someCase — pulled from the user's case table
- When the results instance later asks getElement(42), it hits the cache and gets the casedb's case, NOT its own search-table case
- The two storages are different SQLite tables with independent recordId spaces, but the cache doesn't know that

Why getExternalRoot snapshot saw all-capacity:
- My storage.iterate() hits the SqlStorage directly, bypassing RecordObjectCache
- So it reports the actual table content: 1189 capacity cases

Why the inventory / entity list saw 5 types:
- CaseChildElement.cache() goes through getElement(recordId, context) → hits RecordObjectCache → returns polluted (casedb) data
- The user's casedb has 5 case types, so the polluted results reflect that

The fix (commcare-core):
CaseInstanceTreeElement.getStorageCacheName() must return a key that's unique per storage, not the generic "casedb". Simplest: include the table name or storage identity in the key. E.g.,
return something like "casedb:" + storage.tableName or base it on the instance's getReference().