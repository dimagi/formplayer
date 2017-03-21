package tests;

import beans.NewFormResponse;
import beans.SubmitResponseBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javarosa.core.model.utils.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

import java.util.Date;

/**
 * Regression tests for fixed behaviors
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class EditTest extends BaseTestClass {

    private final Log log = LogFactory.getLog(EditTest.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("editdomain", "editusername");
    }

    // test that we can override today() successfully
    @Test
    public void testFunctionHandlers() throws Throwable {
        NewFormResponse newFormResponse = startNewForm("requests/new_form/edit_form.json", "xforms/edit_form.xml");
        Date date = DateUtils.getDateTimeFromString("2016-11-14T21:24:00.334Z");
        String formattedDate = DateUtils.formatDateToTimeStamp(date);
        assert newFormResponse.getInstanceXml().getOutput().contains("<datetoday>2016-11-14</datetoday>");
        assert newFormResponse.getInstanceXml().getOutput().contains("<datenow>" + formattedDate + "</datenow>");
    }

    // test that we can override today() successfully
    @Test
    public void testEditImage() throws Throwable {
        NewFormResponse newFormResponse = startNewForm("requests/new_form/edit_image.json", "xforms/edit_image.xml");
        assert newFormResponse.getTree()[22].getAnswer().equals("1488964254571.jpg");
    }
}
