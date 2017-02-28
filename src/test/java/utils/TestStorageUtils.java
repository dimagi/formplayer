package utils;

import application.SQLiteProperties;
import database.models.CaseIndexTable;
import engine.FormplayerCaseInstanceTreeElement;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.cases.model.Case;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.xml.CaseXmlParser;
import org.commcare.xml.LedgerXmlParsers;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;
import org.xmlpull.v1.XmlPullParserException;
import session.FormplayerInstanceInitializer;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Hashtable;

/**
 * Created by willpride on 2/21/17.
 */
public class TestStorageUtils {
    /**
     * @return An evaluation context which is capable of evaluating against
     * the connected storage instances: casedb is the only one supported for now
     */
    public static EvaluationContext getEvaluationContextWithoutSession() {
        UserSqlSandbox sandbox = new UserSqlSandbox("testuser", SQLiteProperties.getDataDir() + "test");
        FormplayerInstanceInitializer iif = new FormplayerInstanceInitializer(sandbox);
        return buildEvaluationContext(iif);
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

    public static RuntimeException wrapError(Exception e, String prefix) {
        e.printStackTrace();
        RuntimeException re = new RuntimeException(prefix + ": " + e.getMessage());
        re.initCause(e);
        throw re;
    }

    /**
     * Process an input XML file for transactions and update the relevant databases.
     */
    public static void processResourceTransaction(UserSqlSandbox sandbox, String resourcePath) throws SQLException, ClassNotFoundException {

        DataModelPullParser parser;

        try{
            InputStream is = System.class.getResourceAsStream(resourcePath);

            parser = new DataModelPullParser(is, sandbox, true, true);
            parser.parse();
            is.close();

        } catch(IOException ioe) {
            throw wrapError(ioe, "IO Error parsing transactions");
        } catch (InvalidStructureException e) {
            throw wrapError(e, "Bad Transaction");
        } catch (XmlPullParserException e) {
            throw wrapError(e, "Bad XML");
        } catch (UnfullfilledRequirementsException e) {
            throw wrapError(e, "Bad State");
        }
    }


    /**
     * Get a form instance and case enabled parsing factory
     */
    private static TransactionParserFactory getFactory(UserSqlSandbox sandbox) {
        final Hashtable<String, String> formInstanceNamespaces;
        if (CommCareApplication.instance().getCurrentApp() != null) {
            formInstanceNamespaces = FormSaveUtil.getNamespaceToFilePathMap(CommCareApplication.instance());
        } else {
            formInstanceNamespaces = null;
        }
        return new TransactionParserFactory() {
            @Override
            public TransactionParser getParser(KXmlParser parser) {
                String namespace = parser.getNamespace();
                if (namespace != null && formInstanceNamespaces != null && formInstanceNamespaces.containsKey(namespace)) {
                    return new FormInstanceXmlParser(parser, CommCareApplication.instance(),
                            Collections.unmodifiableMap(formInstanceNamespaces),
                            CommCareApplication.instance().getCurrentApp().fsPath(GlobalConstants.FILE_CC_FORMS));
                } else if(CaseXmlParser.CASE_XML_NAMESPACE.equals(parser.getNamespace()) && "case".equalsIgnoreCase(parser.getName())) {
                    return new CaseXmlParser(parser, sandbox.getCaseStorage(), new EntityStorageCache("case", db), new CaseIndexTable(db)) {
                        @Override
                        protected SQLiteDatabase getDbHandle() {
                            return db;
                        }
                    };
                }  else if (LedgerXmlParsers.STOCK_XML_NAMESPACE.equals(namespace)) {
                    return new LedgerXmlParsers(parser, getLedgerStorage(db));
                }
                return null;
            }
        };
    }
}
