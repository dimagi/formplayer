package hq;

import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.core.sandbox.SandboxUtils;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathFuncExpr;
import beans.CaseFilterRequestBean;

import java.io.File;

/**
 * Created by willpride on 1/7/16.
 */
public class CaseAPIs {

    private static UserSqlSandbox restoreIfNotExists(String username, String xml) throws Exception{
        File db = new File(getDbFilePath(username));
        if(db.exists()){
            return new UserSqlSandbox(username);
        } else{
            return RestoreUtils.restoreUser(username, xml);
        }
    }


    private static String getDbFilePath(String username){
        String path = UserSqlSandbox.DEFAULT_DATBASE_PATH + "/" + username + ".db";
        return path;
    }

    public static String filterCases(CaseFilterRequestBean request) throws Exception{
        try {
            String filterPath = "join(',', instance('casedb')/casedb/case" + request.getFilterExpression() + "/@case_id)";
            UserSqlSandbox mSandbox = restoreIfNotExists(request.getSessionData().getUsername(), request.getRestoreXml());
            EvaluationContext mContext = SandboxUtils.getInstanceContexts(mSandbox, "casedb", "jr://instance/casedb");
            String filteredCases = XPathFuncExpr.toString(XPathParseTool.parseXPath(filterPath).eval(mContext));
            return filteredCases;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "null";
    }
}
