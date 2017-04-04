package tests;

import beans.NewFormResponse;
import beans.SubmitResponseBean;
import beans.menus.EntityListResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import util.Constants;
import utils.TestContext;

import java.util.HashMap;

/**
 * Regression tests for fixed behaviors
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class ParentChildTest extends BaseTestClass{

    @Override
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
        assert submitResponseBean.getStatus().equals(Constants.SYNC_RESPONSE_STATUS_POSITIVE);
        EntityListResponse entityListResponse = sessionNavigate(new String[]{"1"}, "parentchild", EntityListResponse.class);
        assert entityListResponse.getEntities().length == 1;
        String entityId = entityListResponse.getEntities()[0].getId();
        entityListResponse = sessionNavigate(new String[]{"1", entityId}, "parentchild", EntityListResponse.class);
        assert entityListResponse.getEntities().length == 1;
    }
}