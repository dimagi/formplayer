package utils;

import application.SQLiteProperties;
import database.models.CaseIndexTable;
import engine.FormplayerCaseInstanceTreeElement;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.test.utilities.CaseTestUtils;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;
import session.FormplayerInstanceInitializer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;

/**
 * Created by willpride on 2/21/17.
 */
public class TestStorageUtils {
    /**
     * @return An evaluation context which is capable of evaluating against
     * the connected storage instances: casedb is the only one supported for now
     */
    public static EvaluationContext getEvaluationContextWithoutSession() throws SQLException, ClassNotFoundException {
        UserSqlSandbox sandbox = SqlSandboxUtils.getStaticStorage("testuser", SQLiteProperties.getDataDir() + "test");
        Connection connection = getTestConnection(SQLiteProperties.getDataDir() + "test", "testutildb");
        FormplayerInstanceInitializer iif = new FormplayerInstanceInitializer() {
            @Override
            public AbstractTreeElement setupCaseData(ExternalDataInstance instance) {
                SqliteIndexedStorageUtility<Case> storage = sandbox.getCaseStorage();
                FormplayerCaseInstanceTreeElement casebase =
                        new FormplayerCaseInstanceTreeElement(instance.getBase(), storage, new CaseIndexTable());
                instance.setCacheHost(casebase);
                return casebase;
            }
        };

        return buildEvaluationContext(iif);
    }

    private static Connection getTestConnection(String folder, String name) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        SQLiteConnectionPoolDataSource dataSource = new SQLiteConnectionPoolDataSource();
        dataSource.setUrl("jdbc:sqlite:" + folder + "/" + name + ".db");
        return dataSource.getConnection();
    }

    private static EvaluationContext buildEvaluationContext(FormplayerInstanceInitializer iif) {
        ExternalDataInstance edi = new ExternalDataInstance(CaseTestUtils.CASE_INSTANCE, "casedb");
        DataInstance specializedDataInstance = edi.initialize(iif, "casedb");

        ExternalDataInstance ledgerDataInstanceRaw = new ExternalDataInstance(CaseTestUtils.LEDGER_INSTANCE, "ledgerdb");
        DataInstance ledgerDataInstance = ledgerDataInstanceRaw.initialize(iif, "ledger");

        Hashtable<String, DataInstance> formInstances = new Hashtable<>();
        formInstances.put("casedb", specializedDataInstance);
        formInstances.put("ledger", ledgerDataInstance);

        return new EvaluationContext(new EvaluationContext(null), formInstances, TreeReference.rootRef());
    }
}
