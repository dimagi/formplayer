package utils;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

import static junit.framework.Assert.assertEquals;

/**
 * Created by willpride on 3/8/17.
 */
public class DbTestUtils {
    public static void evaluate(String xpath, String expectedValue, EvaluationContext ec) throws XPathSyntaxException {
        XPathExpression expr;
        expr = XPathParseTool.parseXPath(xpath);
        String result = FunctionUtils.toString(expr.eval(ec));
        assertEquals("XPath: " + xpath, expectedValue, result);
    }
}
