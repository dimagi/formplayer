package hq;

import auth.HqAuth;
import beans.CaseBean;
import beans.CaseFilterRequestBean;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.cases.model.Case;
import org.commcare.core.sandbox.SandboxUtils;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathFuncExpr;
import services.RestoreService;

import java.io.File;

/**
 * Created by willpride on 1/7/16.
 */
public class CaseAPIs {

    public static UserSqlSandbox restoreIfNotExists(String username, String xml) throws Exception{
        File db = new File(getDbFilePath(username));
        if(db.exists()){
            return new UserSqlSandbox(username);
        } else{
            return RestoreUtils.restoreUser(username, xml);
        }
    }

    public static UserSqlSandbox restoreIfNotExists(String username, RestoreService restoreService,
                                                    String domain, HqAuth auth) throws Exception{
        File db = new File(getDbFilePath(username));
        if(db.exists()){
            return new UserSqlSandbox(username);
        } else{
            String xml = restoreService.getRestoreXml(domain, auth);
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

    public static CaseBean getFullCase(String caseId, String username){
        UserSqlSandbox mSandbox = new UserSqlSandbox(username);
        SqliteIndexedStorageUtility<Case> caseStorage = mSandbox.getCaseStorage();
        Case cCase = caseStorage.getRecordForValue("case_id", caseId);
        CaseBean ret = new CaseBean(cCase);
        return ret;
    }

    public static CaseBean[] filterCasesFull(CaseFilterRequestBean request) throws Exception{
        String filterPath = "join(',', instance('casedb')/casedb/case" + request.getFilterExpression() + "/@case_id)";
        UserSqlSandbox mSandbox = restoreIfNotExists(request.getSessionData().getUsername(), request.getRestoreXml());
        EvaluationContext mContext = SandboxUtils.getInstanceContexts(mSandbox, "casedb", "jr://instance/casedb");
        String filteredCases = XPathFuncExpr.toString(XPathParseTool.parseXPath(filterPath).eval(mContext));
        String[] splitCases = filteredCases.split(",");
        CaseBean[] ret = new CaseBean[splitCases.length];
        int count = 0;
        for(String cCase: splitCases){
            CaseBean caseBean = CaseAPIs.getFullCase(cCase, request.getSessionData().getUsername());
            ret[count] = caseBean;
            count++;
        }
        return ret;
    }
}
