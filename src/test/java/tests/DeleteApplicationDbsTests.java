package tests;
import beans.NotificationMessage;
import beans.menus.CommandListResponseBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import util.ApplicationUtils;
import utils.TestContext;

import java.io.File;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class DeleteApplicationDbsTests extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("casetestdomain", "casetestuser");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/casetiles.xml";
    }

    /**
     * Ensures that when an application db exists that a delete db request
     * will successfully delete it.
     * @throws Exception
     */
    @Test
    public void testDeleteApplicationDbsView() throws Exception {
        // Create application db by making an install request
        String dbPath = ApplicationUtils.getApplicationDBPath("casetestdomain", "casetestuser", null, "casetestappid");
        CommandListResponseBean menuResponseBean = doInstall("requests/install/install.json");

        File file = new File(dbPath);
        assert file.exists();

        NotificationMessage response = deleteApplicationDbs();
        assert !response.isError();

        file = new File(dbPath);
        assert !file.exists();
    }

    /**
     * Ensures that if no application db exists and a delete db request is
     * made that it successfully returns with status OK.
     * @throws Exception
     */
    @Test
    public void testDeleteApplicationDbsWithNoDbView() throws Exception {
        String dbPath = ApplicationUtils.getApplicationDBPath("casetestdomain", "casetestuser", null, "casetestappid");
        File file = new File(dbPath);
        assert !file.exists();

        NotificationMessage response = deleteApplicationDbs();
        assert !response.isError();

        file = new File(dbPath);
        assert !file.exists();
    }
}
