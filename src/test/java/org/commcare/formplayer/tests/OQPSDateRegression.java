package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.FormEntryResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Regression tests for fixed behaviors
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class OQPSDateRegression extends BaseTestClass {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("oqpsdatedomain", "oqpsusername");
    }

    // test that we can override today() successfully
    @Test
    public void testOQPSSubGroup() throws Throwable {
        NewFormResponse newFormResponse = startNewForm("requests/new_form/new_form_oqps.json",
                "xforms/oqps_date.xml");
        String sessionId = newFormResponse.getSessionId();
        FormEntryResponseBean response = nextScreen(sessionId);
        assert response.getTree().length == 3;
        assert response.getTree()[0].getBinding().equals("/data/question4/question2");
        assert response.getTree()[2].getBinding().equals("/data/question4/question6");
        response = answerQuestionGetResult("1,0", "1", sessionId);
        assert response.getTree().length == 3;
        assert response.getTree()[0].getBinding().equals("/data/question4/question2");
        assert response.getTree()[2].getBinding().equals("/data/question4/question6");
    }
}
