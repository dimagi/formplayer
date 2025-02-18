package org.commcare.formplayer.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.sqlitedb.UserDB;
import org.commcare.formplayer.util.UserUtils;

import org.javarosa.core.model.User;
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

    @Test
    public void testGetUserLocations() {
        String testLocationId = "testLocationId";
        String testDomain = "testDomain";
        User user = new User("test_user", "password_hash", "test_uuid");
        user.setProperty("commcare_location_ids", testLocationId);
        user.setProperty("commcare_project", testDomain);

        UserSqlSandbox sandbox = new UserSqlSandbox(new UserDB("a", "b", null));
        SqlStorage<User> userStorage = sandbox.getUserStorage();
        userStorage.write(user);
        assertEquals(testLocationId, UserUtils.getUserLocationsByDomain(testDomain, sandbox));
    }
}
