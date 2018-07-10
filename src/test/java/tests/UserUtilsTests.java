package tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import util.UserUtils;
import utils.TestContext;
import static org.junit.Assert.assertEquals;

/**
 * Created by benrudolph on 2/7/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class UserUtilsTests {

    @Test
    public void testGetShortUsername() {
        assertEquals("jfk",
                UserUtils.getShortUsername("jfk@us-gov.commcarehq.org", "us-gov"));
        assertEquals("jfk",
                UserUtils.getShortUsername("jfk", "us-gov"));
        assertEquals("dedicatedemployee@example.com",
                UserUtils.getShortUsername("dedicatedemployee@example.com", "example"));
    }
}
