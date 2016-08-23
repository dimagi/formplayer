package tests;
import beans.StatusResponseBean;
import beans.menus.CommandListResponseBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

import java.io.File;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class DeleteApplicationDbsTests extends BaseTestClass{

    /**
     * Ensures that when an application db exists that a delete db request
     * will successfully delete it.
     * @throws Exception
     */
    @Test
    public void testDeleteApplicationDbsView() throws Exception {
        // Create application db by making an install request
        String appId = "casetestappid";
        CommandListResponseBean menuResponseBean = doInstall("requests/install/install.json");

        File file = new File("dbs/" + appId);
        assert file.exists();

        StatusResponseBean response = deleteApplicationDbs();
        assert response.getStatus().equals(StatusResponseBean.OK);

        file = new File("dbs/" + appId);
        assert !file.exists();
    }

    /**
     * Ensures that if no application db exists and a delete db request is
     * made that it successfully returns with status OK.
     * @throws Exception
     */
    @Test
    public void testDeleteApplicationDbsWithNoDbView() throws Exception {
        String appId = "my-app-id";
        File file = new File("dbs/" + appId);
        assert !file.exists();

        StatusResponseBean response = deleteApplicationDbs();
        assert response.getStatus().equals(StatusResponseBean.OK);

        file = new File("dbs/" + appId);
        assert !file.exists();
    }
}
