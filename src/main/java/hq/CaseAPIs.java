package hq;

import application.SQLiteProperties;
import auth.HqAuth;
import beans.AsUserBean;
import beans.CaseBean;
import beans.CaseFilterRequestBean;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.cases.model.Case;
import org.commcare.core.sandbox.SandboxUtils;
import org.commcare.modern.database.TableBuilder;
import org.commcare.modern.parse.ParseUtilsHelper;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.User;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.xmlpull.v1.XmlPullParserException;
import services.RestoreFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by willpride on 1/7/16.
 */
public class CaseAPIs {

    public static UserSqlSandbox forceRestore(RestoreFactory restoreFactory) throws Exception {
        String domain = restoreFactory.getDomain();
        String username = TableBuilder.scrubName(restoreFactory.getUsername());
        String xml = restoreFactory.getRestoreXml();
        new File(SQLiteProperties.getDataDir() + domain + "/" + username + ".db").delete();
        return restoreIfNotExists(username, domain, xml);
    }

    public static UserSqlSandbox restoreIfNotExists(String username, String domain, String xml) throws Exception{
        File db = new File(SQLiteProperties.getDataDir() + domain + "/" + username + ".db");
        if(db.exists()){
            return new UserSqlSandbox(username, SQLiteProperties.getDataDir() + domain);
        } else{
            db.getParentFile().mkdirs();
            return restoreUser(username, SQLiteProperties.getDataDir() + domain,  xml);
        }
    }

    public static UserSqlSandbox restoreIfNotExists(RestoreFactory restoreFactory) throws Exception{
        String domain = restoreFactory.getDomain();
        String username = restoreFactory.getUsername();
        File db = new File(SQLiteProperties.getDataDir() + domain + "/" + username + ".db");
        if(db.exists()){
            return new UserSqlSandbox(username, SQLiteProperties.getDataDir() + domain);
        } else{
            db.getParentFile().mkdirs();
            String xml = restoreFactory.getRestoreXml();
            return restoreUser(username, SQLiteProperties.getDataDir() + domain, xml);
        }
    }

    public static String filterCases(RestoreFactory restoreFactory, String filterExpression) {
        try {
            String filterPath = "join(',', instance('casedb')/casedb/case" + filterExpression + "/@case_id)";
            UserSqlSandbox mSandbox = restoreIfNotExists(restoreFactory);
            EvaluationContext mContext = SandboxUtils.getInstanceContexts(mSandbox, "casedb", "jr://instance/casedb");
            return XPathFuncExpr.toString(XPathParseTool.parseXPath(filterPath).eval(mContext));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "null";
    }

    private static CaseBean getFullCase(String caseId, SqliteIndexedStorageUtility<Case> caseStorage){
        Case cCase = caseStorage.getRecordForValue("case-id", caseId);
        return new CaseBean(cCase);
    }

    public static CaseBean[] filterCasesFull(RestoreFactory restoreFactory, String filterExpression) throws Exception{
        String filterPath = "join(',', instance('casedb')/casedb/case" + filterExpression + "/@case_id)";
        UserSqlSandbox mSandbox = restoreIfNotExists(restoreFactory);
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

    private static UserSqlSandbox restoreUser(String username, String path, String restorePayload) throws
            UnfullfilledRequirementsException, InvalidStructureException, IOException, XmlPullParserException {
        UserSqlSandbox mSandbox = SqlSandboxUtils.getStaticStorage(username, path);
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
        ParseUtilsHelper.parseXMLIntoSandbox(restorePayload, mSandbox);
        // initialize our sandbox's logged in user
        for (IStorageIterator<User> iterator = mSandbox.getUserStorage().iterate(); iterator.hasMore(); ) {
            User u = iterator.nextRecord();
            if (username.equalsIgnoreCase(u.getUsername())) {
                mSandbox    .setLoggedInUser(u);
            }
        }
        return mSandbox;
    }
}
