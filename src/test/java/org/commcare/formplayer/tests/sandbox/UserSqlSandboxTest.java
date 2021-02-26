package org.commcare.formplayer.tests.sandbox;

import org.commcare.core.parse.ParseUtils;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.formplayer.sqlitedb.UserDB;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.User;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the SqlSandsbox API. Just initializes and makes sure we can access at the moment.
 *
 * @author wspride
 */
public class UserSqlSandboxTest {

    private UserSqlSandbox sandbox;
    private final String username = "sandbox-test-user";

    @BeforeEach
    public void setUp() throws Exception {
        new UserDB("a", "b", null).deleteDatabaseFolder();

        UserSqlSandbox sandbox = null;
        try {
            sandbox = new UserSqlSandbox(new UserDB("a", "b", null));
            PrototypeFactory.setStaticHasher(new ClassNameHasher());
            ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().getResourceAsStream("restores/ipm_restore.xml"), sandbox);
        } finally {
            sandbox.getConnection().close();
        }
    }

    @Test
    public void test() throws Exception {
        sandbox = new UserSqlSandbox(new UserDB("a", "b", null));
        assertEquals(sandbox.getCaseStorage().getNumRecords(), 6);
        assertEquals(sandbox.getLedgerStorage().getNumRecords(), 3);
        assertEquals(sandbox.getUserFixtureStorage().getNumRecords(), 4);
        User loggedInUser = sandbox.getLoggedInUser();
        assertEquals(loggedInUser.getUsername(), "test");
    }

    @AfterEach
    public void tearDown() throws SQLException {
        sandbox.getConnection().close();
        new UserDB("a", "b", null).deleteDatabaseFolder();
    }
}
