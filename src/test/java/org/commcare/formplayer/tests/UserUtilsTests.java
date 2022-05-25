package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.commcare.formplayer.util.UserUtils;
import org.junit.jupiter.api.Test;

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
