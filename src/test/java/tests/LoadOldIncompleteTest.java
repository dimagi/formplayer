package tests;

import objects.SerializableFormSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import repo.impl.PostgresMigratedFormSessionRepo;
import util.SessionUtils;
import utils.FileUtils;
import utils.TestContext;

/**
 * Test loading the JSON from an old cloudcare sessions
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class LoadOldIncompleteTest {
    @Test
    public void testLoadIncomplete() {
        String payload = FileUtils.getFile(this.getClass(), "old_incomplete_session.json");
        SerializableFormSession session = PostgresMigratedFormSessionRepo.loadSessionFromJson(payload);
        assert session.getTitle().equals("Surveys > Survey 2");
        assert session.getDomain().equals("normal");
        assert session.getUsername().equals("test");

    }
}
