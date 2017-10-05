package hq;

import api.process.FormRecordProcessorHelper;
import beans.CaseBean;
import engine.FormplayerTransactionParserFactory;
import org.commcare.cases.model.Case;
import org.commcare.core.parse.ParseUtils;
import org.commcare.modern.database.TableBuilder;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.User;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;
import sandbox.SqliteIndexedStorageUtility;
import sandbox.UserSqlSandbox;
import services.RestoreFactory;
import util.UserUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by willpride on 1/7/16.
 */
public class CaseAPIs {

    // This function will only wipe user DBs when they have expired, otherwise will incremental sync
    public static UserSqlSandbox performSync(RestoreFactory restoreFactory) throws Exception {
        // Create parent dirs if needed
        if(restoreFactory.getSqlSandbox().getLoggedInUser() != null){
            restoreFactory.getSQLiteDB().createDatabaseFolder();
        }
        UserSqlSandbox sandbox = restoreUser(restoreFactory, restoreFactory.getRestoreXml());
        FormRecordProcessorHelper.purgeCases(sandbox);
        return sandbox;
    }

    // This function will attempt to get the user DBs without syncing if they exist, sync if not
    public static UserSqlSandbox getSandbox(RestoreFactory restoreFactory) throws Exception {
        if(restoreFactory.getSqlSandbox().getLoggedInUser() != null
                && !restoreFactory.isRestoreXmlExpired()){
            return restoreFactory.getSqlSandbox();
        } else {
            restoreFactory.getSQLiteDB().createDatabaseFolder();
            return restoreUser(restoreFactory, restoreFactory.getRestoreXml());
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
        sandbox.writeSyncToken();
        return sandbox;
    }
}
