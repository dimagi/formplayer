package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.EvaluateXPathResponseBean;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.utils.TestContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class MenuDebuggerTests extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("loaddomain", "loaduser");
    }

    @Test
    public void testMenuDebugger() throws Exception {
        // Menu session should be saved so let's run some menu xpath queries against it
        EvaluateXPathResponseBean evaluateXPathResponseBean = evaluateMenuXPath(
               "requests/evaluate_xpath/evaluate_xpath_menu.json"
        );
        Assert.assertEquals(Constants.ANSWER_RESPONSE_STATUS_POSITIVE, evaluateXPathResponseBean.getStatus());
        // Hack to not have to parse the XML returned
        Assert.assertTrue(evaluateXPathResponseBean.getOutput().contains("15"));
    }
}