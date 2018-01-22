package tests;

import beans.FormEntryResponseBean;
import beans.NewFormResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class SmsFormEntryTest extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("test", "test");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/basic.xml";
    }

    @Test
    public void testOQPS() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/sms/session0/new_form.json", "xforms/oqps.xml");
        FormEntryResponseBean formEntryResponseBean =
                answerQuestionGetResult("requests/sms/session0/answer.json", newSessionResponse.getSessionId());
        assert formEntryResponseBean.getEvent().getType().equals("form-complete");
        assert formEntryResponseBean.getEvent().getIx().equals(">");
    }
}