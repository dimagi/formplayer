package engine;

import org.commcare.api.persistence.UserSqlSandbox;
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

    //final private Context context;
    //final private HttpRequestEndpoints generator;
    final private ArrayList<String> createdAndUpdatedCases = new ArrayList<>();

    private TransactionParserFactory formInstanceParser;
    private boolean caseIndexesWereDisrupted = false;

    /**
     * A mapping from an installed form's namespace its install path.
     */
    private Hashtable<String, String> formInstanceNamespaces;

    public FormplayerTransactionParserFactory(UserSqlSandbox sandbox) {
        super(sandbox);
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
        caseParser = new TransactionParserFactory() {
            CaseXmlParser created = null;

            @Override
            public TransactionParser<Case> getParser(KXmlParser parser) {
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

    public ArrayList<String> getCreatedAndUpdatedCases() {
        return createdAndUpdatedCases;
    }

    public boolean wereCaseIndexesDisrupted() {
        return caseIndexesWereDisrupted;
    }
}
