package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HqUserDetailsTests {

    @Test
    public void testWebUserIsAuthorized() {
        HqUserDetailsBean user = new HqUserDetailsBean(new String[]{"domain", "other-domain"}, "aragorn", false);

        Assertions.assertTrue(user.isAuthorized("domain", "aragorn"));
        Assertions.assertFalse(user.isAuthorized("wrong-domain", "aragorn"));
        Assertions.assertFalse(user.isAuthorized("domain", "wrong-aragorn"));

        HqUserDetailsBean superuser = new HqUserDetailsBean(new String[]{"domain", "other-domain"}, "aragorn", true);
        Assertions.assertTrue(superuser.isAuthorized("wrong-domain", "wrong-aragorn"));
    }

    @Test
    public void testCommCareUserIsAuthorized() {
        HqUserDetailsBean user = new HqUserDetailsBean(new String[]{"domain"}, "bilbo", false);

        Assertions.assertTrue(user.isAuthorized("domain", "bilbo"));
        Assertions.assertFalse(user.isAuthorized("wrong-domain", "bilbo"));
        Assertions.assertFalse(user.isAuthorized("domain", "wrong-bilbo"));
    }
}
