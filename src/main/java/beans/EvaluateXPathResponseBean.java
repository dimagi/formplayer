package beans;

import org.commcare.cases.instance.CaseChildElement;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.engine.XFormPlayer;
import org.javarosa.engine.xml.XmlUtil;
import org.javarosa.model.xform.DataModelSerializer;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathLazyNodeset;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlSerializer;
import session.FormSession;
import util.Constants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

/**
 * Created by willpride on 1/20/16.
 */
public class EvaluateXPathResponseBean {
    private String output;
    private String status;
    private String contentType;

    //Jackson requires the default constructor
    public EvaluateXPathResponseBean(){}

    public EvaluateXPathResponseBean(EvaluationContext evaluationContext, String xpath) throws XPathSyntaxException {
        status = Constants.ANSWER_RESPONSE_STATUS_POSITIVE;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        KXmlSerializer serializer = new KXmlSerializer();

        try {
            XPathExpression  expr = XPathParseTool.parseXPath(xpath);
            Object value = expr.eval(evaluationContext);

            // Wrap output in a top level node
            serializer.setOutput(outputStream, "UTF-8");
            serializer.startTag(null, "result");
            serializer.flush();
            XPathExpression.serializeResult(value, outputStream);
            serializer.endTag(null, "result");
            serializer.flush();
            output = XmlUtil.getPrettyXml(outputStream.toByteArray());
        } catch (XPathException | XPathSyntaxException e) {
            status = Constants.ANSWER_RESPONSE_STATUS_NEGATIVE;
            output = e.getMessage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString(){
        return "EvaluateXPathResponseBean: [output=" + output + "]";
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
