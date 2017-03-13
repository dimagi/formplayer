package tests;

import beans.NewFormResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

/**
 * Regression tests for fixed behaviors
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class EditTest extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("editdomain", "editusername");
    }

    // test that we can override today() successfully
    @Test
    public void testFunctionHandlers() throws Throwable {
        NewFormResponse newFormResponse = startNewForm("requests/new_form/edit_form.json", "xforms/edit_form.xml");
        assert newFormResponse.getInstanceXml().getOutput().contains("<datetoday>2017-02-13</datetoday>");
    }

    // test that we can override today() successfully
    @Test
    public void testEditImage() throws Throwable {
        NewFormResponse newFormResponse = startNewForm("requests/new_form/edit_image.json", "xforms/edit_image.xml");
        assert newFormResponse.getTree()[22].getAnswer().equals("1488964254571.jpg");
    }
}
