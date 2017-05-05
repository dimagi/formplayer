package tests;

import org.javarosa.core.model.condition.EvaluationContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import sandbox.UserSqlSandbox;
import utils.TestContext;
import utils.TestStorageUtils;
import static utils.DbTestUtils.evaluate;

/**
 * @author wspride
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CaseDbQueryTest extends BaseTestClass {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("synctestdomain", "synctestusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/case_create.xml";
    }

    /**
     * Tests for basic common case database queries
     */
    @Test
    public void testBasicCaseQueries() throws Exception {
        syncDb();
        UserSqlSandbox sandbox = restoreFactoryMock.getSqlSandbox();
        EvaluationContext ec = TestStorageUtils.getEvaluationContextWithoutSession(sandbox);
        evaluate("count(instance('casedb')/casedb/case[@case_id = 'test_case_id'])", "1", ec);
        evaluate("instance('casedb')/casedb/case[@case_id = 'test_case_id']/case_name", "Test Case", ec);
        evaluate("instance('casedb')/casedb/case[@case_id = 'test_case_id']/case_name", "Test Case", ec);
        evaluate("instance('casedb')/casedb/case[@case_id = 'test_case_id']/test_value", "initial", ec);
        evaluate("instance('casedb')/casedb/case[@case_id = 'test_case_id']/missing_value", "", ec);
    }
}
