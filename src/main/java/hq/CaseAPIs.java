package hq;

import beans.CaseBean;
import engine.FormplayerTransactionParserFactory;
import sandbox.SqlSandboxUtils;
import sandbox.SqliteIndexedStorageUtility;
import sandbox.UserSqlSandbox;
import org.commcare.cases.model.Case;
import org.commcare.core.parse.ParseUtils;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.User;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;
import services.RestoreFactory;
import util.PropertyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by willpride on 1/7/16.
 */
public class CaseAPIs {

    public static UserSqlSandbox forceRestore(RestoreFactory restoreFactory) throws Exception {
        SqlSandboxUtils.deleteDatabaseFolder(restoreFactory.getDbFile());
        restoreFactory.closeConnection();
        return restoreIfNotExists(restoreFactory, true);
    }

    public static UserSqlSandbox restoreIfNotExists(RestoreFactory restoreFactory, boolean overwriteCache) throws Exception{
        if (restoreFactory.isRestoreXmlExpired()) {
            SqlSandboxUtils.deleteDatabaseFolder(restoreFactory.getDbFile());;
        }
        if(restoreFactory.getSqlSandbox().getLoggedInUser() != null){
            return restoreFactory.getSqlSandbox();
        } else{
            new File(restoreFactory.getDbFile()).getParentFile().mkdirs();
            InputStream xml = restoreFactory.getRestoreXml(overwriteCache);
            return restoreUser(restoreFactory, xml);
        }
    }

    public static CaseBean getFullCase(String caseId, SqliteIndexedStorageUtility<Case> caseStorage){
        Case cCase = caseStorage.getRecordForValue("case-id", caseId);
        return new CaseBean(cCase);
    }

    private static UserSqlSandbox restoreUser(RestoreFactory restoreFactory, InputStream restorePayload) throws
            UnfullfilledRequirementsException, InvalidStructureException, IOException, XmlPullParserException {
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
        UserSqlSandbox sandbox = restoreFactory.getSqlSandbox();
        FormplayerTransactionParserFactory factory = new FormplayerTransactionParserFactory(sandbox, true);
        restoreFactory.setAutoCommit(false);
        ParseUtils.parseIntoSandbox(restorePayload, factory, true, true);
        restoreFactory.commit();
        restoreFactory.setAutoCommit(true);
        // initialize our sandbox's logged in user
        for (IStorageIterator<User> iterator = sandbox.getUserStorage().iterate(); iterator.hasMore(); ) {
            User u = iterator.nextRecord();
            if (restoreFactory.getWrappedUsername().equalsIgnoreCase(u.getUsername())) {
                sandbox.setLoggedInUser(u);
            }
        }
        return sandbox;
    }
}
