package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class EndOfFormNavFormLinkingTests extends BaseTestClass {

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("form_linkingdomain", "form_linkingusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/case_with_usercase.xml";
    }

    private HashMap<String, Object> getAnswers(String index, String answer) {
        HashMap<String, Object> ret = new HashMap<>();
        ret.put(index, answer);
        return ret;
    }

    @Test
    public void testFormLinkingFromRegistrationToFollowup() throws Exception {
        NewFormResponse response =
                sessionNavigate(new String[]{"0", "0"},
                        "form_linking", NewFormResponse.class);
        SubmitResponseBean submitResponse = submitForm(
                getAnswers("0", "bart"),
                response.getSessionId()
        );
        NewFormResponse formResponse = getNextScreenForEOFNavigation(submitResponse, NewFormResponse.class);
        assertEquals("Followup Form 2", formResponse.getTitle());
    }

    /**
     * Test form linking with xpath expression evaluates to true
     */
    @Test
    public void testFormLinkingFromFollowupForm1ToFollowupForm2() throws Exception {
        NewFormResponse response =
                sessionNavigate(new String[]{"1", "5ece8407-4d16-49fc-83d9-c044f08fc708", "0"},
                        "form_linking", NewFormResponse.class);
        HashMap<String, Object> answers = getAnswers("0", "bart");  // name question
        answers.put("1", "followup2");  // next_form question
        SubmitResponseBean submitResponse = submitForm(answers, response.getSessionId());
        NewFormResponse formResponse = getNextScreenForEOFNavigation(submitResponse, NewFormResponse.class);
        assertEquals("Followup Form 2", formResponse.getTitle());
    }

    /**
     * Test form linking with xpath expression evaluates to false
     */
    @Test
    public void testFormLinkingFromFollowupForm1ToRoot() throws Exception {
        NewFormResponse response =
                sessionNavigate(new String[]{"1", "5ece8407-4d16-49fc-83d9-c044f08fc708", "0"},
                        "form_linking", NewFormResponse.class);
        HashMap<String, Object> answers = getAnswers("0", "homer");  // name question
        answers.put("1", "");  // 'next_form' question
        SubmitResponseBean submitResponse = submitForm(answers, response.getSessionId());
        assertNull(submitResponse.getNextScreen());
    }
}
