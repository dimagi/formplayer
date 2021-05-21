package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.auth.FeatureFlagChecker;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.utils.TestContext;
import org.commcare.formplayer.utils.WithHqUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
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
    @WithHqUser
    public void testUserHasRole() {
        Assertions.assertTrue(FeatureFlagChecker.isPreviewEnabled("preview_a"));
        Assertions.assertTrue(FeatureFlagChecker.isPreviewEnabled("preview_b"));
        Assertions.assertFalse(FeatureFlagChecker.isPreviewEnabled("preview_c"));
        Assertions.assertTrue(FeatureFlagChecker.isToggleEnabled("toggle_a"));
        Assertions.assertTrue(FeatureFlagChecker.isToggleEnabled("toggle_b"));
        Assertions.assertFalse(FeatureFlagChecker.isToggleEnabled("toggle_c"));
    }
}
