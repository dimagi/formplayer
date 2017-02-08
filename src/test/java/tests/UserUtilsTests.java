package tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import util.UserUtils;
import utils.TestContext;

/**
 * Created by benrudolph on 2/7/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class UserUtilsTests {

    @Test
    public void testIsAnonymous() {
        Assert.assertFalse(UserUtils.isAnonymous("domain", "not-anonymous@gmail.com"));
        Assert.assertFalse(UserUtils.isAnonymous("domain", UserUtils.anonymousUsername("wrong-domain")));
        Assert.assertTrue(UserUtils.isAnonymous("domain", UserUtils.anonymousUsername("domain")));
    }
}
