package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.EvaluateXPathResponseBean;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class MenuDebuggerTests extends BaseTestClass {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("loaddomain", "loaduser");
    }

    @Test
    public void testMenuDebugger() throws Exception {
        // Menu session should be saved so let's run some menu xpath queries against it
        EvaluateXPathResponseBean evaluateXPathResponseBean = evaluateMenuXpath(
                "requests/evaluate_xpath/evaluate_xpath_menu.json"
        );
        Assertions.assertEquals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE,
                evaluateXPathResponseBean.getStatus());
        // Hack to not have to parse the XML returned
        Assertions.assertTrue(evaluateXPathResponseBean.getOutput().contains("15"));
    }
}
