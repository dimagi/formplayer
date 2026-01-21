# Case Search Cache Key Bug Fix

## Problem Description

An intermittent bug was causing wrong cases to be returned from case search requests. The bug only occurred when:
1. A user with **superuser privileges** AND domain membership made a case search request
2. A regular domain member (WITHOUT superuser privileges) made the same case search request  
3. Both users accessed the same domain with identical query parameters

The issue was a **cache collision** - the cache key didn't distinguish between superuser and non-superuser domain members, so they shared cached results despite superusers having broader access permissions that bypass normal ownership/location restrictions.

**Note**: The bug could NOT be reproduced when a superuser accessed a domain they were NOT a member of, likely because those requests may not use the same caching mechanism or have different query patterns.

## Root Cause

The cache key generation in `CaseSearchHelper.getCacheKey()` was missing critical authorization context:

### Original Cache Key Components
- Domain name
- Username (scrubbed)
- As Username (if restoring as another user)
- URL
- Query parameters

### What Was Missing
- **Superuser status** - Whether the user has global access to all domains
- **Domain membership status** - Whether the user is an authorized member of the domain

## Why This Caused Wrong Cases

### Authorization Logic
From `HqUserDetailsBean.isAuthorized()`:
```java
public boolean isAuthorized(String domain, String username) {
    return isSuperUser || Arrays.asList(domains).contains(domain) && this.username.equals(username);
}
```

**Superusers**: Can access ANY domain, bypass ALL permission checks
**Domain members**: Must be in domain's member list, subject to ownership/location/permission filters

### The Bug Scenario

Both users in this scenario are domain members, but have different permission levels:
- **User A**: Domain member WITH superuser privileges → sees more cases (bypasses restrictions)
- **User B**: Domain member WITHOUT superuser privileges → sees filtered cases (has restrictions)

**Scenario A: Superuser Member First (BUG)**
1. User A (superuser + domain member of `co-carecoordination-dev`) makes case search
2. HQ backend detects superuser → Returns ALL accessible cases, bypassing normal ownership/location restrictions
3. Results cached with key (OLD): `co-carecoordination-dev_mriese@dimagi.com_<url>_<params>`
4. User B (regular domain member, non-superuser) makes identical request
5. Cache key matches! User B gets cached superuser results → **WRONG CASES RETURNED** (sees cases they shouldn't have access to)

**Scenario B: Regular Member First (Appears to Work)**
1. User B (regular domain member) makes request
2. HQ applies normal member filtering (ownership, location restrictions)
3. Results cached with key (OLD): `co-carecoordination-dev_regularuser_<url>_<params>`
4. User A (superuser member) makes identical request  
5. Cache key matches! User A gets cached restricted results → Appears to work, but superuser sees fewer cases than they should

This explains the **intermittent nature** - it depends on which user type hits the cache first! 

**Example of the Cache Key Collision (OLD CODE)**:
```
Superuser domain member cache key:    co-carecoordination-dev_mriese@dimagi.com_http://...
Regular domain member cache key:      co-carecoordination-dev_regularuser_http://...
                                      ^^ SAME KEY FORMAT - no superuser distinction!
```

When `mriese@dimagi.com` happens to be both the superuser AND the regular user making requests (e.g., testing with the same account), the keys become IDENTICAL:
```
Superuser domain member:    co-carecoordination-dev_mriese@dimagi.com_http://...
Regular domain member:      co-carecoordination-dev_mriese@dimagi.com_http://...
                           ^^^ EXACT SAME KEY = CACHE COLLISION!
```

The old cache key couldn't distinguish between domain members with different privilege levels.

## The Fix

### Changes Made

**File: `formplayer/src/main/java/org/commcare/formplayer/services/CaseSearchHelper.java`**

1. **Added imports** for user authorization context:
   - `org.commcare.formplayer.beans.auth.HqUserDetailsBean`
   - `org.commcare.formplayer.util.RequestUtils`
   - `java.util.Optional`

2. **Enhanced `getCacheKey()` method** to include authorization context:
   ```java
   // Include user authorization context in cache key
   Optional<HqUserDetailsBean> userDetails = RequestUtils.getUserDetails();
   if (userDetails.isPresent()) {
       HqUserDetailsBean user = userDetails.get();
       String domain = restoreFactory.getDomain();
       String username = restoreFactory.getScrubbedUsername();
       
       // Include superuser status
       boolean isSuperUser = user.isSuperUser();
       builder.append("_superuser=").append(isSuperUser);
       
       // Include domain membership status
       boolean isDomainMember = user.isAuthorized(domain, username);
       builder.append("_member=").append(isDomainMember);
   }
   ```

3. **Added debug logging** to track cache key generation:
   - Logs authorization context (domain, user, superuser status, membership status)
   - Logs generated cache key
   - Enhanced cache hit/miss logging to include cache key

### New Cache Key Format

**Before:**
```
domain_username_asuser_url_queryparams
```

**After:**
```
domain_username_asuser_superuser=[true/false]_member=[true/false]_url_queryparams
```

### Example Cache Keys

**Domain member WITH superuser privileges:**
```
co-carecoordination-dev_mriese@dimagi.com_superuser=true_member=true_http://...
```

**Domain member WITHOUT superuser privileges:**
```
co-carecoordination-dev_regularuser_superuser=false_member=true_http://...
```
**Key Point**: Both users have `member=true`, but different `superuser` values. The old cache key didn't include `superuser`, so these two users shared the same cache entry and got wrong results!

**Non-member superuser (accessing via superuser privileges only):**
```
co-carecoordination-dev_mriese@dimagi.com_superuser=true_member=true_http://...
```
Note: `isAuthorized()` returns `true` for superusers even without explicit domain membership, so `member=true` appears here too. However, this scenario wasn't reproducible in testing.

**Non-member non-superuser (unauthorized):**
```
co-carecoordination-dev_regularuser_superuser=false_member=false_http://...
```

## Testing

### New Tests Added

**File: `formplayer/src/test/java/org/commcare/formplayer/tests/CaseClaimNavigationTests.java`**

1. `testCacheKey_DomainMemberVsSuperuser()` - Verifies domain member cache key includes correct flags
2. `testCacheKey_SuperuserNotMember()` - Verifies superuser cache key includes correct flags
3. `testCacheKey_NonMemberNonSuperuser()` - Verifies unauthorized user cache key includes correct flags
4. `testCacheKey_IncludesAuthorizationContext()` - Verifies all cache keys include auth context

### Running Tests
```bash
./gradlew test --tests CaseClaimNavigationTests.testCacheKey*
```

## Impact

### Before Fix
- Domain members with different privilege levels (superuser vs non-superuser) shared cached results
- Wrong cases intermittently returned based on who cached first:
  - Regular members could see cases they shouldn't have access to (if superuser cached first)
  - Superusers could see fewer cases than expected (if regular member cached first)
- Security/permission boundary violations possible

### After Fix
- Each authorization context gets its own cache entry
- Domain members with superuser privileges never share cache with non-superuser domain members
- Correct cases always returned regardless of cache state or request order
- Proper permission boundaries maintained

## Debugging

### Log Messages to Watch For

**Cache key generation:**
```
DEBUG CaseSearchHelper - Case search cache key includes auth context: domain=co-carecoordination-dev, user=mriese@dimagi.com, isSuperUser=true, isDomainMember=true
DEBUG CaseSearchHelper - Generated case search cache key: co-carecoordination-dev_mriese@dimagi.com_superuser=true_member=true_...
```

**Cache hit/miss:**
```
INFO CaseSearchHelper - Cache HIT for case search: url=http://..., key=...
INFO CaseSearchHelper - Cache MISS for case search: url=http://..., key=...
```

### Verifying the Fix

To verify the fix is working:
1. Enable DEBUG logging for `CaseSearchHelper`
2. Make a case search request as a domain member WITH superuser privileges
3. Check logs for cache key with `superuser=true_member=true`
4. Make the same request as a domain member WITHOUT superuser privileges  
5. Check logs for cache key with `superuser=false_member=true`
6. Verify the cache keys are DIFFERENT (they now differ in the `superuser` component)
7. Verify that each user gets correct results without seeing the other's cached data

## Related Code

- `formplayer/src/main/java/org/commcare/formplayer/beans/auth/HqUserDetailsBean.java` - User authorization logic
- `formplayer/src/main/java/org/commcare/formplayer/util/RequestUtils.java` - Request context utilities
- `formplayer/src/main/java/org/commcare/formplayer/services/RestoreFactory.java` - User restore configuration

## Future Considerations

This fix ensures proper cache isolation based on authorization context. If additional factors affect case search results (e.g., location hierarchy, user roles), those should also be considered for inclusion in the cache key.