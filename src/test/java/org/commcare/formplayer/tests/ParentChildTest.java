package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression tests for fixed behaviors
 */
@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class ParentChildTest extends BaseTestClass {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("parentchilddomain", "parentchildusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/edit_user_data.xml";
    }

    @Test
    public void testParentChild() throws Throwable {
        NewFormResponse newFormResponse = sessionNavigate(new String[]{"0", "0"}, "parentchild", NewFormResponse.class);
        HashMap<String, Object> answers = new HashMap<>();
        answers.put("0", "Sr");
        answers.put("1", "Jr");
        SubmitResponseBean submitResponseBean = submitForm(answers, newFormResponse.getSessionId());
        assertEquals(Constants.SUBMIT_RESPONSE_STATUS_POSITIVE, submitResponseBean.getStatus());
        EntityListResponse entityListResponse = sessionNavigate(new String[]{"1"}, "parentchild", EntityListResponse.class);
        assertEquals(1, entityListResponse.getEntities().length);
        String entityId = entityListResponse.getEntities()[0].getId();
        entityListResponse = sessionNavigate(new String[]{"1", entityId}, "parentchild", EntityListResponse.class);
        assertEquals(1, entityListResponse.getEntities().length);
    }
}
