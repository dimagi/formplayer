package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.auth.FeatureFlagChecker;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.utils.HqUserDetails;
import org.commcare.formplayer.utils.WithHqUserSecurityContextFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

public class HqUserDetailsTests {

    @Test
    public void testWebUserIsAuthorized() {
        HqUserDetailsBean user = new HqUserDetailsBean("domain",
                new String[]{"domain", "other-domain"}, "aragorn",
                false, new String[]{}, new String[]{});

        Assertions.assertTrue(user.isAuthorized("domain", "aragorn"));
        Assertions.assertFalse(user.isAuthorized("wrong-domain", "aragorn"));
        Assertions.assertFalse(user.isAuthorized("domain", "wrong-aragorn"));

        HqUserDetailsBean superuser = new HqUserDetailsBean("domain",
                new String[]{"domain", "other-domain"}, "aragorn",
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
    public void testFeatureFlagChecker_isToggleEnabled() {
        WithHqUserSecurityContextFactory.setSecurityContext(
                HqUserDetails.builder().enabledToggles(new String[]{"toggle_a", "toggle_b"}).build()
        );
        Assertions.assertTrue(FeatureFlagChecker.isToggleEnabled("toggle_a"));
        Assertions.assertTrue(FeatureFlagChecker.isToggleEnabled("toggle_b"));
        Assertions.assertFalse(FeatureFlagChecker.isToggleEnabled("toggle_c"));
    }

    @Test
    public void testFeatureFlagChecker_isPreviewEnabled() {
        WithHqUserSecurityContextFactory.setSecurityContext(
                HqUserDetails.builder().enabledPreviews(new String[]{"preview_a", "preview_b"}).build()
        );
        Assertions.assertTrue(FeatureFlagChecker.isPreviewEnabled("preview_a"));
        Assertions.assertTrue(FeatureFlagChecker.isPreviewEnabled("preview_b"));
        Assertions.assertFalse(FeatureFlagChecker.isPreviewEnabled("preview_c"));
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }
}
