package hq;

import beans.CaseBean;
import engine.FormplayerTransactionParserFactory;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.api.persistence.UserSqlSandbox;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by willpride on 1/7/16.
 */
public class CaseAPIs {

    public static UserSqlSandbox forceRestore(RestoreFactory restoreFactory) throws Exception {
        SqlSandboxUtils.deleteDatabaseFolder(restoreFactory.getDbFile());
        return restoreIfNotExists(restoreFactory, true);
    }

    public static UserSqlSandbox restoreIfNotExists(RestoreFactory restoreFactory, boolean overwriteCache) throws Exception{
        if (restoreFactory.isRestoreXmlExpired()) {
            SqlSandboxUtils.deleteDatabaseFolder(restoreFactory.getDbFile());
        }
        File db = new File(restoreFactory.getDbFile());
        if(db.exists()){
            return restoreFactory.getSqlSandbox();
        } else{
            db.getParentFile().mkdirs();
            InputStream xml = restoreFactory.getRestoreXml(overwriteCache);
            return restoreUser(restoreFactory.getWrappedUsername(), restoreFactory.getDbPath(), xml);
        }
    }

    public static UserSqlSandbox restoreIfNotExists(String username, String asUsername, String domain) throws Exception {
        // This is a shitty hack to allow serialized sessions to use the RestoreFactory path methods.
        // We need a refactor of the entire infrastructure
        RestoreFactory restoreFactory = new RestoreFactory();
        restoreFactory.setDomain(domain);
        restoreFactory.setUsername(username);
        restoreFactory.setAsUsername(asUsername);
        return restoreIfNotExists(restoreFactory, false);
    }

    public static CaseBean getFullCase(String caseId, SqliteIndexedStorageUtility<Case> caseStorage){
        Case cCase = caseStorage.getRecordForValue("case-id", caseId);
        return new CaseBean(cCase);
    }

    private static UserSqlSandbox restoreUser(String username, String path, InputStream restorePayload) throws
            UnfullfilledRequirementsException, InvalidStructureException, IOException, XmlPullParserException {
        UserSqlSandbox sandbox = new UserSqlSandbox(username, path);
        PrototypeFactory.setStaticHasher(new ClassNameHasher());

        FormplayerTransactionParserFactory factory = new FormplayerTransactionParserFactory(sandbox);

        ParseUtils.parseXMLIntoSandbox(restorePayload, factory);
        // initialize our sandbox's logged in user
        for (IStorageIterator<User> iterator = sandbox.getUserStorage().iterate(); iterator.hasMore(); ) {
            User u = iterator.nextRecord();
            if (username.equalsIgnoreCase(u.getUsername())) {
                sandbox.setLoggedInUser(u);
            }
        }
        return sandbox;
    }
}
