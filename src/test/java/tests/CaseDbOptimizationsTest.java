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
import utils.TestContext;
import utils.TestStorageUtils;

import static junit.framework.Assert.assertEquals;

/**
 * @author wspride
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class CaseDbOptimizationsTest extends BaseTestClass {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        configureRestoreFactory("synctestdomain", "synctestusername");
    }

    @Override
    protected String getMockRestoreFileName() {
        return "restores/dbtests/case_test_db_optimizations.xml";
    }

    /**
     * Tests for basic common case database queries
     */
    @Test
    public void testDbOptimizations() throws Exception {
        syncDb();
        EvaluationContext ec = TestStorageUtils.getEvaluationContextWithoutSession();
        ec.setDebugModeOn();
        evaluate("join(',',instance('casedb')/casedb/case[index/parent = 'test_case_parent']/@case_id)", "child_one,child_two,child_three", ec);
        evaluate("join(',',instance('casedb')/casedb/case[index/parent = 'test_case_parent'][@case_id = 'child_two']/@case_id)", "child_two", ec);
        evaluate("join(',',instance('casedb')/casedb/case[index/parent = 'test_case_parent'][@case_id != 'child_two']/@case_id)", "child_one,child_three", ec);

        evaluate("join(',',instance('casedb')/casedb/case[selected('test_case_parent', index/parent)]/@case_id)", "child_one,child_two,child_three", ec);
        evaluate("join(',',instance('casedb')/casedb/case[selected('test_case_parent test_case_parent_2', index/parent)]/@case_id)", "child_one,child_two,child_three", ec);
        evaluate("join(',',instance('casedb')/casedb/case[selected('test_case_parent_2 test_case_parent', index/parent)]/@case_id)", "child_one,child_two,child_three", ec);
        evaluate("join(',',instance('casedb')/casedb/case[selected('test_case_parent_2 test_case_parent_3', index/parent)]/@case_id)", "", ec);
        evaluate("join(',',instance('casedb')/casedb/case[selected('', index/parent)]/@case_id)", "", ec);
    }

    public static void evaluate(String xpath, String expectedValue, EvaluationContext ec) {
        XPathExpression expr;
        try {
            expr = XPathParseTool.parseXPath(xpath);
            String result = FunctionUtils.toString(expr.eval(ec));
            assertEquals("XPath: " + xpath, expectedValue, result);
        } catch (XPathSyntaxException e) {
            e.printStackTrace();
        }
    }


}
