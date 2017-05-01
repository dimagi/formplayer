package engine;

import parsers.FormplayerBulkCaseXmlParser;
import sandbox.UserSqlSandbox;
import org.commcare.cases.model.Case;
import org.commcare.core.parse.CommCareTransactionParserFactory;
import org.commcare.data.xml.TransactionParser;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.xml.CaseXmlParser;
import org.kxml2.io.KXmlParser;
import parsers.FormplayerCaseXmlParser;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by willpride on 3/6/17.
 */
public class FormplayerTransactionParserFactory extends CommCareTransactionParserFactory {

    final private ArrayList<String> createdAndUpdatedCases = new ArrayList<>();

    private TransactionParserFactory formInstanceParser;
    private boolean caseIndexesWereDisrupted = false;

    /**
     * A mapping from an installed form's namespace its install path.
     */
    private Hashtable<String, String> formInstanceNamespaces;

    public FormplayerTransactionParserFactory(UserSqlSandbox sandbox, boolean useBulkProcessing) {
        super(sandbox, useBulkProcessing);
    }

    public UserSqlSandbox getSqlSandbox() {
        return (UserSqlSandbox) sandbox;
    }

    @Override
    public TransactionParser getParser(KXmlParser parser) {
        String namespace = parser.getNamespace();
        if (namespace != null && formInstanceNamespaces != null && formInstanceNamespaces.containsKey(namespace)) {
            req();
            return formInstanceParser.getParser(parser);
        }
        return super.getParser(parser);
    }

    @Override
    public void initCaseParser() {
        if (isBulkProcessingEnabled) {
            caseParser = getBulkCaseParser();
        } else {
            caseParser = getNormalCaseParser();
        }
    }

    @Override
    public TransactionParserFactory getNormalCaseParser() {
        return new TransactionParserFactory() {
            FormplayerCaseXmlParser created = null;

            @Override
            public FormplayerCaseXmlParser getParser(KXmlParser parser) {
                if (created == null) {
                    created = new FormplayerCaseXmlParser(parser, true, (UserSqlSandbox) sandbox) {

                        @Override
                        public void onIndexDisrupted(String caseId) {
                            caseIndexesWereDisrupted = true;
                        }

                        @Override
                        protected void onCaseCreateUpdate(String caseId) {
                            createdAndUpdatedCases.add(caseId);
                        }
                    };
                }

                return created;
            }
        };
    }

    @Override
    public TransactionParserFactory getBulkCaseParser() {
        return new TransactionParserFactory() {
            FormplayerBulkCaseXmlParser created = null;

            @Override
            public FormplayerBulkCaseXmlParser getParser(KXmlParser parser) {
                if (created == null) {
                    created = new FormplayerBulkCaseXmlParser(parser, (UserSqlSandbox)sandbox) {

                        @Override
                        public void onIndexDisrupted(String caseId) {
                            caseIndexesWereDisrupted = true;
                        }

                        @Override
                        protected void onCaseCreateUpdate(String caseId) {
                            createdAndUpdatedCases.add(caseId);
                        }
                    };
                }

                return created;
            }
        };
    }

    public ArrayList<String> getCreatedAndUpdatedCases() {
        return createdAndUpdatedCases;
    }

    public boolean wereCaseIndexesDisrupted() {
        return caseIndexesWereDisrupted;
    }
}
