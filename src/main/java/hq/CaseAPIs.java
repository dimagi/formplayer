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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by willpride on 1/7/16.
 */
public class CaseAPIs {

    public static UserSqlSandbox forceRestore(RestoreFactory restoreFactory) throws Exception {
        SqlSandboxUtils.deleteDatabaseFolder(restoreFactory.getDbFile());
        UserSqlSandbox.closeConnection();
        return restoreIfNotExists(restoreFactory);
    }

    public static UserSqlSandbox restoreIfNotExists(RestoreFactory restoreFactory) throws Exception{
        if (restoreFactory.isRestoreXmlExpired()) {
            SqlSandboxUtils.deleteDatabaseFolder(restoreFactory.getDbFile());
        }
        if(restoreFactory.getSqlSandbox().getLoggedInUser() != null){
            return restoreFactory.getSqlSandbox();
        } else{
            new File(restoreFactory.getDbFile()).getParentFile().mkdirs();
            InputStream stream = restoreFactory.getRestoreStream();
            return restoreUser(restoreFactory.getSqlSandbox(), restoreFactory.getWrappedUsername(), stream);
        }
    }

    public static UserSqlSandbox restoreIfNotExists(String username, String asUsername, String domain, String xml) throws Exception {
        // This is a shitty hack to allow serialized sessions to use the RestoreFactory path methods.
        // We need a refactor of the entire infrastructure
        RestoreFactory restoreFactory = new RestoreFactory();
        restoreFactory.setDomain(domain);
        restoreFactory.setUsername(username);
        restoreFactory.setAsUsername(asUsername);
        restoreFactory.setCachedRestore(xml);
        return restoreIfNotExists(restoreFactory);
    }

    public static CaseBean getFullCase(String caseId, SqliteIndexedStorageUtility<Case> caseStorage){
        Case cCase = caseStorage.getRecordForValue("case-id", caseId);
        return new CaseBean(cCase);
    }

    private static UserSqlSandbox restoreUser(UserSqlSandbox sandbox, String username, InputStream stream) throws
            UnfullfilledRequirementsException, InvalidStructureException, IOException, XmlPullParserException {
        PrototypeFactory.setStaticHasher(new ClassNameHasher());

        FormplayerTransactionParserFactory factory = new FormplayerTransactionParserFactory(sandbox);

        System.out.println("Parsing...");
        sandbox.setAutoCommit(false);
        ParseUtils.parseIntoSandbox(stream, false, factory);
        sandbox.commit();
        System.out.println("I am committed");
        sandbox.setAutoCommit(true);

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
