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
import requests.FilterRequest;
import requests.RestoreRequest;

import java.io.File;
import java.io.IOException;
import java.rmi.server.ExportException;
import java.util.logging.Filter;

/**
 * Created by willpride on 1/7/16.
 */
public class CaseAPIs {

    private static UserSqlSandbox restoreIfNotExists(RestoreRequest restoreRequest) throws Exception{
        File db = new File(getDbFilePath(restoreRequest.getUsername()));
        if(db.exists()){
            return new UserSqlSandbox(restoreRequest.getUsername());
        } else{
            return RestoreUtils.restoreUser(restoreRequest);
        }
    }


    private static String getDbFilePath(String username){
        String path = UserSqlSandbox.DEFAULT_DATBASE_PATH + "/" + username + ".db";
        return path;
    }

    public static String filterCases(FilterRequest request) throws Exception{
        try {
            String filterPath = "join(',', instance('casedb')/casedb/case" + request.getFilterExpression() + "/@case_id)";
            UserSqlSandbox mSandbox = restoreIfNotExists(request.getRestoreRequest());
            EvaluationContext mContext = SandboxUtils.getInstanceContexts(mSandbox, "casedb", "jr://instance/casedb");
            String filteredCases = XPathFuncExpr.toString(XPathParseTool.parseXPath(filterPath).eval(mContext));
            return filteredCases;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "null";
    }
}
