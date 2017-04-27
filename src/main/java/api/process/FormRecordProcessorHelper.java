package api.process;

import engine.FormplayerTransactionParserFactory;
import org.commcare.core.process.XmlFormRecordProcessor;
import org.commcare.data.xml.TransactionParserFactory;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *  * Convenience methods, mostly for Touchforms so we don't have to deal with Java IO
 * in Jython which is terrible
 *
 * Created by wpride1 on 8/20/15.
 */
public class FormRecordProcessorHelper extends XmlFormRecordProcessor {

    public static void processXML(FormplayerTransactionParserFactory factory, String fileText) throws IOException, XmlPullParserException, UnfullfilledRequirementsException, InvalidStructureException {
        InputStream stream = new ByteArrayInputStream(fileText.getBytes("UTF-8"));
        process(stream, factory);
        if (factory.wereCaseIndexesDisrupted()) {

        }
    }
}
