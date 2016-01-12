package hq;

import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.core.sandbox.SandboxUtils;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathLazyNodeset;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathFuncExpr;

import java.io.IOException;
import java.rmi.server.ExportException;

/**
 * Created by willpride on 1/7/16.
 */
public class CaseAPIs {

    public static String filterCases(String username, String password){
        return filterCases(username, password, "");
    }

    public static String filterCases(String username, String password, String filters){
        try {
            String filterPath = "join(',', instance('casedb')/casedb/case" + filters + "/@case_id)";
            UserSqlSandbox mSandbox = RestoreUtils.restoreUser(username, password);
            EvaluationContext mContext = SandboxUtils.getInstanceContexts(mSandbox, "casedb", "jr://instance/casedb");
            String filteredCases = XPathFuncExpr.toString(XPathParseTool.parseXPath(filterPath).eval(mContext));
            return filteredCases;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "null";
    }
}
