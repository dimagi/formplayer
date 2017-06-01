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

    //Jackson requires the default constructor
    public EvaluateXPathResponseBean(){}

    public EvaluateXPathResponseBean(FormSession formEntrySession, String xpath) throws XPathSyntaxException {
        status = Constants.ANSWER_RESPONSE_STATUS_POSITIVE;
        EvaluationContext evaluationContext = formEntrySession.getFormEntryModel().getForm().getEvaluationContext();

        try {
            XPathExpression  expr = XPathParseTool.parseXPath(xpath);
            Object val = expr.eval(evaluationContext);

            if (isCaseElement(val)) {
                output = serializeCaseElement((XPathNodeset) val);
            } else {
                output = XFormPlayer.getDisplayString(val);
            }
        } catch (XPathException | XPathSyntaxException e) {
            status = Constants.ANSWER_RESPONSE_STATUS_NEGATIVE;
            output = e.getMessage();
        }
    }

    private boolean isCaseElement(Object value) {
        if (!(value instanceof XPathNodeset)) {
            return false;
        }
        XPathNodeset nodeset = (XPathNodeset) value;
        Vector<TreeReference> refs = nodeset.getReferences();
        if (refs.size() != 1) {
            return false;
        }

        DataInstance instance = ((XPathLazyNodeset) value).getInstance();
        AbstractTreeElement treeElement = instance.resolveReference(refs.get(0));
        if (!(treeElement instanceof CaseChildElement)) {
            return false;
        }
        return true;
    }

    private String serializeCaseElement(XPathNodeset nodeset) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        KXmlSerializer serializer = new KXmlSerializer();

        try {
            serializer.setOutput(outputStream, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DataModelSerializer s = new DataModelSerializer(serializer);

        Vector<TreeReference> refs = nodeset.getReferences();
        DataInstance instance = nodeset.getInstance();
        AbstractTreeElement treeElement = instance.resolveReference(refs.get(0));

        try {
            s.serialize(treeElement);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
}
