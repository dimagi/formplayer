package parsers;

import org.commcare.xml.CaseXmlParser;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.kxml2.io.KXmlParser;

/**
 * Created by willpride on 3/6/17.
 */
public class FormplayerCaseXmlParser extends CaseXmlParser {
    public FormplayerCaseXmlParser(KXmlParser parser, boolean acceptCreateOverwrites, IStorageUtilityIndexed storage) {
        super(parser, acceptCreateOverwrites, storage);
    }
}
