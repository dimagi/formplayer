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