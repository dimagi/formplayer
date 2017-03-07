package tests;

import beans.NewFormResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestContext;

/**
 * Regression tests for model iteration not persisting
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class ModelIterationTests extends BaseTestClass{

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("modeliterationdomain", "modeliterationusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/modeliteration.xml";
    }


    @Test
    public void testReportModule() throws Exception{
        // Just ensure that this doesn't error
        NewFormResponse response = sessionNavigate(new String[]{"0",
                "c83da43c-5222-46fd-b6a6-df8ad35e7e76",
                "6c52f3ef-c322-4c46-a005-37d45a2c13cb",
                "1"}, "modeliteration", NewFormResponse.class);
        answerQuestionGetResult("1_0,1,1", "1", response.getSessionId());
    }
}
