package services;

import engine.FormplayerTransactionParserFactory;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;
import sandbox.UserSqlSandbox;

import java.io.IOException;

/**
 * Created by willpride on 8/30/17.
 */
public interface RecordProcessorService {

    void processXML(FormplayerTransactionParserFactory factory, String fileText)
            throws IOException, XmlPullParserException, UnfullfilledRequirementsException, InvalidStructureException;

    void purgeCases(UserSqlSandbox sandbox);

    UserSqlSandbox performSync(RestoreFactory restoreFactory) throws Exception;
}
