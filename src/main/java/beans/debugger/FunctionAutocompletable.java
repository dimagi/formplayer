package beans.debugger;

import org.javarosa.xpath.expr.XPathFuncExpr;

/**
 * Represents a function that can be used to autocomplete XPath entry.
 */
public class FunctionAutocompletable extends AutoCompletableItem {
    public FunctionAutocompletable(Class clazz) {
        super(clazz.getName(), clazz.getName(), "Function");
        try {
            XPathFuncExpr function = (XPathFuncExpr) clazz.newInstance();
            int argCount = function.getExpectedArgCount();
            String name = function.getName();
            StringBuilder sb = new StringBuilder(name);
            sb.append("(");
            if(argCount > 0) {
                sb.append("${0}");
            }
            if(argCount > 1) {
                for(int i = 0; i < argCount - 1; i ++) {
                    sb.append(", ${");
                    sb.append(i);
                    sb.append("}");
                }
            }
            sb.append(")");
            setValue(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
