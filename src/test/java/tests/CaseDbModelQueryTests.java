package tests;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import sandbox.UserSqlSandbox;
import utils.TestContext;
import utils.TestStorageUtils;

import static junit.framework.Assert.assertEquals;
import static utils.DbTestUtils.evaluate;

/**
 * @author wspride
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CaseDbModelQueryTests extends BaseTestClass {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("synctestdomain", "synctestusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/dbtests/case_db_model_query.xml";
    }

    /**
     * Tests for basic common case database queries
     */
    @Test
    public void testDbModelQueryLookups() throws Exception {
        syncDb();
        UserSqlSandbox sandbox = restoreFactoryMock.getSqlSandbox();
        EvaluationContext ec = TestStorageUtils.getEvaluationContextWithoutSession(sandbox);
        ec.setDebugModeOn();
        evaluate("join(',',instance('casedb')/casedb/case[@case_type='unit_test_child_child'][@status='open'][true() and " +
                "instance('casedb')/casedb/case[@case_id = instance('casedb')/casedb/case[@case_id=current()/index/parent]/index/parent]/test = 'true']/@case_id)", "child_ptwo_one_one,child_one_one", ec);
        evaluate("join(',',instance('casedb')/casedb/case[@case_type='unit_test_child'][@status='open'][true() and " +
                "count(instance('casedb')/casedb/case[index/parent = instance('casedb')/casedb/case[@case_id=current()/@case_id]/index/parent][false = 'true']) > 0]/@case_id)", "", ec);

    }
}
