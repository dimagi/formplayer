package tests;

import beans.FormEntryResponseBean;
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
public class OQPSDateRegression extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("oqpsdatedomain", "oqpsusername");
    }

    // test that we can override today() successfully
    @Test
    public void testOQPSSubGroup() throws Throwable {
        NewFormResponse newFormResponse = startNewForm("requests/new_form/new_form_oqps.json", "xforms/oqps_date.xml");
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
