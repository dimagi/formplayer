package tests;

import hq.models.CommCareUser;
import hq.models.WebUser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

/**
 * Test class for WebUser and CommCareUser
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CouchUserTests {

    @Test
    public void testWebUserIsAuthorized() {
        WebUser user = new WebUser(new String[]{"domain", "other-domain"}, "aragorn", false);

        Assert.assertTrue(user.isAuthorized("domain", "aragorn"));
        Assert.assertFalse(user.isAuthorized("wrong-domain", "aragorn"));
        Assert.assertFalse(user.isAuthorized("domain", "wrong-aragorn"));

        WebUser superuser = new WebUser(new String[]{"domain", "other-domain"}, "aragorn", true);
        Assert.assertTrue(superuser.isAuthorized("wrong-domain", "wrong-aragorn"));
    }

    @Test
    public void testCommCareUserIsAuthorized() {
        CommCareUser user = new CommCareUser("domain", "bilbo");

        Assert.assertTrue(user.isAuthorized("domain", "bilbo"));
        Assert.assertFalse(user.isAuthorized("wrong-domain", "bilbo"));
        Assert.assertFalse(user.isAuthorized("domain", "wrong-bilbo"));
    }
}
