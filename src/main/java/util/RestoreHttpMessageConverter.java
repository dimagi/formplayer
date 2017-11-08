package util;

import engine.FormplayerTransactionParserFactory;
import exceptions.AsyncRetryException;
import org.apache.commons.io.IOUtils;
import org.commcare.core.parse.ParseUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * HttpMessageConverter subclass specifically for parsing UserSandboxes from restore payload.
 * This class allows us to parse the stream directly from HQ rather than copying the stream to an intermediary.
 * Instead of returning an object, we parse the information into the passed-in factory class.
 * In the event of an async restore being triggered, throws an AsyncRetryException
 *
 * @author wpride
 */
public class RestoreHttpMessageConverter extends AbstractHttpMessageConverter<Void> {

    FormplayerTransactionParserFactory factory;

    public RestoreHttpMessageConverter(FormplayerTransactionParserFactory factory) {
        super(MediaType.ALL);
        this.factory = factory;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Void.class == clazz;
    }

    @Override
    protected Void readInternal(Class<? extends Void> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {

        if (inputMessage instanceof ClientHttpResponse) {
            if (((ClientHttpResponse) inputMessage).getRawStatusCode() == 202) {
                String responseBody = null;
                try {
                    responseBody = IOUtils.toString(inputMessage.getBody(), "utf-8");
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read async restore response", e);
                }
                handleAsyncRestoreResponse(responseBody, inputMessage.getHeaders());
            }
        }

        try {
            ParseUtils.parseIntoSandbox(inputMessage.getBody(), factory, true, true);
        } catch (InvalidStructureException | UnfullfilledRequirementsException | XmlPullParserException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Given an async restore xml response, this function throws an AsyncRetryException
     * with meta data about the async restore.
     *
     * @param xml - Async restore response
     * @param headers - HttpHeaders from the restore response
     */
    public static void handleAsyncRestoreResponse(String xml, HttpHeaders headers) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        ByteArrayInputStream input;
        Document doc;

        // Create the XML Document builder
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Unable to instantiate document builder");
        }

        // Parse the xml into a utf-8 byte array
        try {
            input = new ByteArrayInputStream(xml.getBytes("utf-8") );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to parse async restore response.");
        }

        // Build an XML document
        try {
            doc = builder.parse(input);
        } catch (SAXException e) {
            throw new RuntimeException("Unable to parse into XML Document");
        } catch (IOException e) {
            throw new RuntimeException("Unable to parse into XML Document");
        }

        NodeList messageNodes = doc.getElementsByTagName("message");
        NodeList progressNodes = doc.getElementsByTagName("progress");

        assert messageNodes.getLength() == 1;
        assert progressNodes.getLength() == 1;

        String message = messageNodes.item(0).getTextContent();
        Node progressNode = progressNodes.item(0);
        NamedNodeMap attributes = progressNode.getAttributes();

        throw new AsyncRetryException(
                message,
                Integer.parseInt(attributes.getNamedItem("done").getTextContent()),
                Integer.parseInt(attributes.getNamedItem("total").getTextContent()),
                Integer.parseInt(headers.get("retry-after").get(0))
        );
    }

    @Override
    protected void writeInternal(Void object, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        throw new RuntimeException("Can't write a SqlSandbox");
    }

}
