package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.commcare.formplayer.utils.TestContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class HqUserDetailsTests {

    @Test
    public void testWebUserIsAuthorized() {
        HqUserDetailsBean user = new HqUserDetailsBean(new String[]{"domain", "other-domain"}, "aragorn", false);

        Assert.assertTrue(user.isAuthorized("domain", "aragorn"));
        Assert.assertFalse(user.isAuthorized("wrong-domain", "aragorn"));
        Assert.assertFalse(user.isAuthorized("domain", "wrong-aragorn"));

        HqUserDetailsBean superuser = new HqUserDetailsBean(new String[]{"domain", "other-domain"}, "aragorn", true);
        Assert.assertTrue(superuser.isAuthorized("wrong-domain", "wrong-aragorn"));
    }

    @Test
    public void testCommCareUserIsAuthorized() {
        HqUserDetailsBean user = new HqUserDetailsBean(new String[]{"domain"}, "bilbo", false);

        Assert.assertTrue(user.isAuthorized("domain", "bilbo"));
        Assert.assertFalse(user.isAuthorized("wrong-domain", "bilbo"));
        Assert.assertFalse(user.isAuthorized("domain", "wrong-bilbo"));
    }
}
