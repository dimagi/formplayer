package tests;

import objects.SerializableFormSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import util.SessionUtils;
import utils.FileUtils;
import utils.TestContext;

/**
 * Tests for FormplayerDateUtils
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class LoadOldIncompleteTest {
    @Test
    public void testLoadIncomplete() {
        String path = FileUtils.getFile(this.getClass(), "old_incomplete_session.json");
        SerializableFormSession session = SessionUtils.loadSessionFromJson(path);
        System.out.println("Session: " + session);
    }
}
