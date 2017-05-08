package tests;

import beans.RunQARequestBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.FileUtils;
import utils.TestContext;

/**
 * Regression tests for fixed behaviors
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class QaRunnerTests extends BaseTestClass {

    @Autowired
    private TestRestTemplate restTemplate;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("qarunnerdomain", "qarunnerusername");
    }
    @Test
    public void runBasicQa() throws Exception {
        String testPlan = FileUtils.getFile(this.getClass(), "qaplans/basic.feature");
        RunQARequestBean requestBean = mapper.readValue(
                FileUtils.getFile(this.getClass(), "requests/run_qa/basic.json"),
                RunQARequestBean.class);
        requestBean.setQaPlan(testPlan);
        String body = this.restTemplate.getForObject(mapper.writeValueAsString(requestBean),
                String.class);
    }
}
