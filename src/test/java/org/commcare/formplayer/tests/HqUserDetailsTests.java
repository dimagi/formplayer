package org.commcare.formplayer.tests;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    public void testIsAuthorizedForDomain() {
        HqUserDetailsBean user = new HqUserDetailsBean("domain",
                new String[]{"domain", "other-domain"}, "aragorn",
                false, new String[]{}, new String[]{});

        Assertions.assertTrue(user.isAuthorizedForDomain("domain"));
        Assertions.assertTrue(user.isAuthorizedForDomain("other-domain"));
        Assertions.assertFalse(user.isAuthorizedForDomain("wrong-domain"));
    }

    @Test
    public void testIsAuthorizedIgnoresPublicSessionFlag() {
        HqUserDetailsBean publicUser = new HqUserDetailsBean("domain",
                new String[]{"domain"}, "public_abc123@domain.commcarehq.org",
                false, new String[]{}, new String[]{});
        publicUser.setPublicSession(true);

        Assertions.assertTrue(publicUser.isAuthorized("domain", "public_abc123@domain.commcarehq.org"));
        Assertions.assertFalse(publicUser.isAuthorized("domain", "some-other-name"));
        Assertions.assertFalse(publicUser.isAuthorized("other-domain", "public_abc123@domain.commcarehq.org"));
    }

    @Test
    public void testPublicSessionDeserialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // HQ sends the reserved word `public` for a public web apps session, along with the
        // authoritative app id and session endpoint the link is bound to.
        HqUserDetailsBean publicUser = mapper.readValue(
                "{\"username\":\"pub\",\"public\":true,"
                        + "\"app_build_id\":\"app-1\",\"endpoint_id\":\"ep-1\"}",
                HqUserDetailsBean.class);
        Assertions.assertTrue(publicUser.isPublicSession());
        Assertions.assertEquals("app-1", publicUser.getPublicAppId());
        Assertions.assertEquals("ep-1", publicUser.getPublicEndpointId());

        HqUserDetailsBean regularUser = mapper.readValue(
                "{\"username\":\"reg\",\"public\":false}", HqUserDetailsBean.class);
        Assertions.assertFalse(regularUser.isPublicSession());

        // Absent `public` defaults to false (primitive boolean; the bean also ignores unknowns).
        // The app/endpoint fields are reference types, so they default to null.
        HqUserDetailsBean noField = mapper.readValue(
                "{\"username\":\"reg\"}", HqUserDetailsBean.class);
        Assertions.assertFalse(noField.isPublicSession());
        Assertions.assertNull(noField.getPublicAppId());
        Assertions.assertNull(noField.getPublicEndpointId());
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
