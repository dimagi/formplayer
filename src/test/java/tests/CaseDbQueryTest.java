package tests;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import utils.TestStorageUtils;

import static junit.framework.Assert.assertEquals;

/**
 * @author wspride
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class CaseDbQueryTest {

    @Before
    public void setupTests() {
        TestStorageUtils.initializeStaticTestStorage();
    }

    /**
     * Tests for basic common case database queries
     */
    @Test
    public void testBasicCaseQueries() {
        TestStorageUtils.processResourceTransaction("/inputs/case_create.xml");

        EvaluationContext ec = TestStorageUtils.getEvaluationContextWithoutSession();

        evaluate("count(instance('casedb')/casedb/case[@case_id = 'test_case_id'])", "1", ec);
        evaluate("instance('casedb')/casedb/case[@case_id = 'test_case_id']/case_name", "Test Case", ec);
        evaluate("instance('casedb')/casedb/case[@case_id = 'test_case_id']/case_name", "Test Case", ec);
        evaluate("instance('casedb')/casedb/case[@case_id = 'test_case_id']/test_value", "initial", ec);
        evaluate("instance('casedb')/casedb/case[@case_id = 'test_case_id']/missing_value", "", ec);
    }

    /**
     * Tests for basic common case index related queries
     */
    @Test
    public void testCaseIndexQueries() {
        TestStorageUtils.processResourceTransaction("/inputs/case_create.xml");
        TestStorageUtils.processResourceTransaction("/inputs/case_create_and_index.xml");

        EvaluationContext ec = TestStorageUtils.getEvaluationContextWithoutSession();

        evaluate("instance('casedb')/casedb/case[@case_id = 'test_case_id_child']/index/parent", "test_case_id", ec);
        evaluate("instance('casedb')/casedb/case[@case_id = 'test_case_id']/index/missing", "", ec);

        // TODO PLM: only tests how the case data instance is initialized using
        // the test framework (TestUtils.getInstanceBackedEvaluationContext).
        // We need to create robolectric tests that perform this evaluation
        // during form entry
        evaluate("instance('casedb')/casedb/case[@case_type = 'unit_test_child'][index/parent = 'test_case_id_2']/@case_id",
                "test_case_id_child_2", ec);
    }

    @Test
    public void testCaseOptimizationTriggers() {
        TestStorageUtils.processResourceTransaction("/inputs/case_test_db_optimizations.xml");
        EvaluationContext ec = TestStorageUtils.getEvaluationContextWithoutSession();

        evaluate("join(',',instance('casedb')/casedb/case[index/parent = 'test_case_parent']/@case_id)", "child_one,child_two,child_three", ec);
        evaluate("join(',',instance('casedb')/casedb/case[index/parent = 'test_case_parent'][@case_id = 'child_two']/@case_id)", "child_two", ec);
        evaluate("join(',',instance('casedb')/casedb/case[index/parent = 'test_case_parent'][@case_id != 'child_two']/@case_id)", "child_one,child_three", ec);
    }


    @Test
    public void testIndexSetMemberOptimizations() {
        TestStorageUtils.processResourceTransaction("/inputs/case_test_db_optimizations.xml");
        EvaluationContext ec = TestStorageUtils.getEvaluationContextWithoutSession();

        evaluate("join(',',instance('casedb')/casedb/case[selected('test_case_parent', index/parent)]/@case_id)", "child_one,child_two,child_three", ec);
        evaluate("join(',',instance('casedb')/casedb/case[selected('test_case_parent test_case_parent_2', index/parent)]/@case_id)", "child_one,child_two,child_three", ec);
        evaluate("join(',',instance('casedb')/casedb/case[selected('test_case_parent_2 test_case_parent', index/parent)]/@case_id)", "child_one,child_two,child_three", ec);
        evaluate("join(',',instance('casedb')/casedb/case[selected('test_case_parent_2 test_case_parent_3', index/parent)]/@case_id)", "", ec);
        evaluate("join(',',instance('casedb')/casedb/case[selected('', index/parent)]/@case_id)", "", ec);
    }

    @Test
    public void testModelQueryLookupDerivations() {
        TestStorageUtils.processResourceTransaction("/inputs/case_test_model_query_lookups.xml");
        EvaluationContext ec = TestStorageUtils.getEvaluationContextWithoutSession();

        evaluate("join(',',instance('casedb')/casedb/case[@case_type='unit_test_child_child'][@status='open'][true() and " +
                "instance('casedb')/casedb/case[@case_id = instance('casedb')/casedb/case[@case_id=current()/index/parent]/index/parent]/test = 'true']/@case_id)", "child_ptwo_one_one,child_one_one", ec);

    }

    @Test
    public void testModelSelfReference() {
        TestStorageUtils.processResourceTransaction("/inputs/case_test_model_query_lookups.xml");
        EvaluationContext ec = TestStorageUtils.getEvaluationContextWithoutSession();

        evaluate("join(',',instance('casedb')/casedb/case[@case_type='unit_test_child'][@status='open'][true() and " +
                "count(instance('casedb')/casedb/case[index/parent = instance('casedb')/casedb/case[@case_id=current()/@case_id]/index/parent][false = 'true']) > 0]/@case_id)", "", ec);

    }


    public static void evaluate(String xpath, String expectedValue, EvaluationContext ec) {
        XPathExpression expr;
        try {
            expr = XPathParseTool.parseXPath(xpath);
            String result = FunctionUtils.toString(expr.eval(ec));
            assertEquals("XPath: " + xpath, expectedValue, result);
        } catch (XPathSyntaxException e) {
            TestStorageUtils.wrapError(e, "XPath: " + xpath);
        }
    }

}
