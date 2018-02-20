package parsers;

import org.commcare.resources.model.ResourceTable;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.xml.SuiteParser;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Currently, we have to skip offline (demo) restores for Formplayer because these restore
 * payloads are usually too large to store in SQLite and we don't have a file system
 * storage abstraction setup.
 */
public class FormplayerSuiteParser extends SuiteParser {

    public FormplayerSuiteParser(InputStream suiteStream, ResourceTable table, String resourceGuid, IStorageUtilityIndexed<FormInstance> fixtureStorage) throws IOException {
        super(suiteStream, table, resourceGuid, fixtureStorage);
    }

    @Override
    protected void handleTag(String tagName,
                             Hashtable<String, Entry> entries,
                             Vector<Menu> menus,
                             Hashtable<String, Detail> details) throws IOException, XmlPullParserException, InvalidStructureException, UnfullfilledRequirementsException {
        switch(tagName) {
            case "user-restore":
                parser.nextTag();
                break;
            default:
                super.handleTag(tagName, entries, menus, details);
        }
    }
}
