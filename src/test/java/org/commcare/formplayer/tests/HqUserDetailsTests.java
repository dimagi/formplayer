package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.auth.FeatureFlagChecker;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HqUserDetailsTests {

    @Test
    public void testWebUserIsAuthorized() {
        HqUserDetailsBean user = new HqUserDetailsBean("domain", new String[]{"domain", "other-domain"}, "aragorn",
                false, new String[]{}, new String[]{});

        Assertions.assertTrue(user.isAuthorized("domain", "aragorn"));
        Assertions.assertFalse(user.isAuthorized("wrong-domain", "aragorn"));
        Assertions.assertFalse(user.isAuthorized("domain", "wrong-aragorn"));

        HqUserDetailsBean superuser = new HqUserDetailsBean("domain", new String[]{"domain", "other-domain"}, "aragorn",
                true, new String[]{}, new String[]{});
        Assertions.assertTrue(superuser.isAuthorized("wrong-domain", "wrong-aragorn"));
    }

    @Test
    public void testCommCareUserIsAuthorized() {
        HqUserDetailsBean user = new HqUserDetailsBean("domain", new String[]{"domain"}, "bilbo",
                false, new String[]{}, new String[]{});

        Assertions.assertTrue(user.isAuthorized("domain", "bilbo"));
        Assertions.assertFalse(user.isAuthorized("wrong-domain", "bilbo"));
        Assertions.assertFalse(user.isAuthorized("domain", "wrong-bilbo"));
    }

    @Test
    public void testUserHasRole() {
        String[] enabledToggles = new String[]{"toggle_a", "toggle_b"};
        String[] enabledPreviews = new String[]{"preview_a", "preview_b"};
        HqUserDetailsBean user = new HqUserDetailsBean("domain", new String[]{"domain"}, "bilbo",
                false, enabledToggles, enabledPreviews);
        Assertions.assertTrue(FeatureFlagChecker.INSTANCE.isPreviewEnabled("preview_a", user));
        Assertions.assertTrue(FeatureFlagChecker.INSTANCE.isPreviewEnabled("preview_b", user));
        Assertions.assertFalse(FeatureFlagChecker.INSTANCE.isPreviewEnabled("preview_c", user));
        Assertions.assertTrue(FeatureFlagChecker.INSTANCE.isToggleEnabled("toggle_a", user));
        Assertions.assertTrue(FeatureFlagChecker.INSTANCE.isToggleEnabled("toggle_b", user));
        Assertions.assertFalse(FeatureFlagChecker.INSTANCE.isToggleEnabled("toggle_c", user));
    }
}
