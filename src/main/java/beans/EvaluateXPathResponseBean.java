package beans;

import org.commcare.cases.instance.CaseChildElement;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.engine.XFormPlayer;
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

        try {
            XPathExpression  expr = XPathParseTool.parseXPath(xpath);
            Object val = expr.eval(evaluationContext);

            if (!isLeafNode(val)) {
                output = serializeElements((XPathNodeset) val);
                contentType = "text/xml";
            } else {
                output = XFormPlayer.getDisplayString(val);
                contentType = "text/plain";
            }
        } catch (XPathException | XPathSyntaxException e) {
            status = Constants.ANSWER_RESPONSE_STATUS_NEGATIVE;
            output = e.getMessage();
            contentType = "text/plain";
        }
    }

    private boolean isLeafNode(Object value) {
        if (!(value instanceof XPathNodeset)) {
            return false;
        }
        XPathNodeset nodeset = (XPathNodeset) value;
        Vector<TreeReference> refs = nodeset.getReferences();
        if (refs == null || refs.size() != 1) {
            return false;
        }

        DataInstance instance = ((XPathLazyNodeset) value).getInstance();
        AbstractTreeElement treeElement = instance.resolveReference(refs.get(0));
        return treeElement.getNumChildren() == 0;
    }

    private String serializeElements(XPathNodeset nodeset) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        KXmlSerializer serializer = new KXmlSerializer();

        try {
            serializer.setOutput(outputStream, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DataModelSerializer s = new DataModelSerializer(serializer);

        DataInstance instance = nodeset.getInstance();
        Vector<TreeReference> refs = nodeset.getReferences();
        for (TreeReference ref : refs) {
            AbstractTreeElement treeElement = instance.resolveReference(ref);

            try {
                s.serialize(treeElement);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return outputStream.toString();
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
