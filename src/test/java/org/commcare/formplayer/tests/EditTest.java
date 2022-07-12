package org.commcare.formplayer.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.utils.TestContext;
import org.javarosa.core.model.utils.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Date;

/**
 * Regression tests for fixed behaviors
 */
@WebMvcTest
public class EditTest extends BaseTestClass {

    private final Log log = LogFactory.getLog(EditTest.class);

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("editdomain", "editusername");
    }

    // test that we can override today() successfully
    @Test
    public void testFunctionHandlers() throws Throwable {
        NewFormResponse newFormResponse = startNewForm("requests/new_form/edit_form.json",
                "xforms/edit_form.xml");
        Date date = DateUtils.getDateTimeFromString("2016-11-14T21:24:00.334Z");
        String formattedDate = DateUtils.formatDateToTimeStamp(date);
        assert newFormResponse.getInstanceXml().getOutput().contains(
                "<datetoday>2016-11-14</datetoday>");
        assert newFormResponse.getInstanceXml().getOutput().contains(
                "<datenow>" + formattedDate + "</datenow>");
    }

    // test that we can override today() successfully
    @Test
    public void testEditImage() throws Throwable {
        NewFormResponse newFormResponse = startNewForm("requests/new_form/edit_image.json",
                "xforms/edit_image.xml");
        assert newFormResponse.getTree()[22].getAnswer().equals("1488964254571.jpg");
    }
}
