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

    public static UserSqlSandbox restoreIfNotExists(String username, String domain, String xml) throws Exception{
        File db = new File(UserSqlSandbox.DEFAULT_DATBASE_PATH + "/" + domain + "/" + username + ".db");
        if(db.exists()){
            return new UserSqlSandbox(username, UserSqlSandbox.DEFAULT_DATBASE_PATH + "/" + domain);
        } else{
            db.getParentFile().mkdirs();
            return RestoreUtils.restoreUser(username,UserSqlSandbox.DEFAULT_DATBASE_PATH + "/" + domain,  xml);
        }
    }

    public static UserSqlSandbox restoreIfNotExists(String username, RestoreService restoreService,
                                                    String domain, HqAuth auth) throws Exception{
        File db = new File(UserSqlSandbox.DEFAULT_DATBASE_PATH + "/" + domain + "/" + username + ".db");
        if(db.exists()){
            return new UserSqlSandbox(username, UserSqlSandbox.DEFAULT_DATBASE_PATH + "/" + domain);
        } else{
            db.getParentFile().mkdirs();
            String xml = restoreService.getRestoreXml(domain, auth);
            return RestoreUtils.restoreUser(username, UserSqlSandbox.DEFAULT_DATBASE_PATH + "/" + domain, xml);
        }
    }

    public static String filterCases(CaseFilterRequestBean request) throws Exception{
        try {
            String filterPath = "join(',', instance('casedb')/casedb/case" + request.getFilterExpression() + "/@case_id)";
            UserSqlSandbox mSandbox = restoreIfNotExists(
                    request.getSessionData().getUsername(),
                    request.getSessionData().getDomain(),
                    request.getRestoreXml());
            EvaluationContext mContext = SandboxUtils.getInstanceContexts(mSandbox, "casedb", "jr://instance/casedb");
            String filteredCases = XPathFuncExpr.toString(XPathParseTool.parseXPath(filterPath).eval(mContext));
            return filteredCases;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "null";
    }

    private static CaseBean getFullCase(String caseId, SqliteIndexedStorageUtility<Case> caseStorage){
        Case cCase = caseStorage.getRecordForValue("case_id", caseId);
        CaseBean ret = new CaseBean(cCase);
        return ret;
    }

    public static CaseBean[] filterCasesFull(CaseFilterRequestBean request) throws Exception{
        String filterPath = "join(',', instance('casedb')/casedb/case" + request.getFilterExpression() + "/@case_id)";
        UserSqlSandbox mSandbox = restoreIfNotExists(
                request.getSessionData().getUsername(),
                request.getSessionData().getDomain(),
                request.getRestoreXml());
        EvaluationContext mContext = SandboxUtils.getInstanceContexts(mSandbox, "casedb", "jr://instance/casedb");
        String filteredCases = XPathFuncExpr.toString(XPathParseTool.parseXPath(filterPath).eval(mContext));
        String[] splitCases = filteredCases.split(",");
        CaseBean[] ret = new CaseBean[splitCases.length];
        SqliteIndexedStorageUtility<Case> caseStorage = mSandbox.getCaseStorage();
        int count = 0;
        for(String cCase: splitCases){
            CaseBean caseBean = CaseAPIs.getFullCase(cCase, caseStorage);
            ret[count] = caseBean;
            count++;
        }
        return ret;
    }
}
