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
        String test_loc_id = "test_location_id";
        String test_domain = "test_domain";
        User user = new User("test_user", "password_hash", "test_uuid");
        user.setProperty("commcare_location_ids", test_loc_id);
        user.setProperty("commcare_project", test_domain);

        UserSqlSandbox sandbox = new UserSqlSandbox(new UserDB("a", "b", null));
        SqlStorage<User> userStorage = sandbox.getUserStorage();
        userStorage.write(user);
        assertEquals(test_loc_id, UserUtils.getUserLocationsByDomain(test_domain, sandbox));
    }
}
