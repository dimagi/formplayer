package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.FormEntryResponseBean;
import org.commcare.formplayer.beans.NewFormResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.commcare.formplayer.utils.TestContext;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class SmsFormEntryTest extends BaseTestClass{

    @Override
    @BeforeEach
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

    @Test
    public void testMultipleQuestions() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/sms/session1/new_form.json", "xforms/oqps.xml");
        assert newSessionResponse.getEvent().getType().equals("question");
        assert newSessionResponse.getEvent().getIx().equals("0");
        assert newSessionResponse.getEvent().getCaption().equals("Text 1");
        FormEntryResponseBean formEntryResponseBean =
                answerQuestionGetResult("requests/sms/session1/answer_0.json", newSessionResponse.getSessionId());
        assert formEntryResponseBean.getEvent().getType().equals("question");
        assert formEntryResponseBean.getEvent().getIx().equals("1");
        assert formEntryResponseBean.getEvent().getCaption().equals("Text 2");
        formEntryResponseBean =
                answerQuestionGetResult("requests/sms/session1/answer_1.json", newSessionResponse.getSessionId());
        assert formEntryResponseBean.getEvent().getType().equals("form-complete");
        assert formEntryResponseBean.getEvent().getIx().equals(">");
    }

    @Test
    public void testFieldList() throws Exception {
        NewFormResponse newSessionResponse = startNewForm("requests/sms/session2/new_form.json", "xforms/field_list.xml");
        FormEntryResponseBean formEntryResponseBean =
                answerQuestionGetResult("requests/sms/session1/answer_0.json", newSessionResponse.getSessionId());
        assert formEntryResponseBean.getEvent().getType().equals("sub-group");
        formEntryResponseBean = nextScreen(newSessionResponse.getSessionId(), true);
        assert formEntryResponseBean.getEvent().getCaption().contains("First");
        formEntryResponseBean =
                answerQuestionGetResult("requests/sms/session1/answer_1.json", newSessionResponse.getSessionId());
    }
}