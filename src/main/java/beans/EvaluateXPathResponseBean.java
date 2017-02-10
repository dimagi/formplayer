package beans;

import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.engine.XFormPlayer;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import session.FormSession;
import util.Constants;

import java.util.Hashtable;

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
            output = XFormPlayer.getDisplayString(val);
        } catch (XPathException | XPathSyntaxException e) {
            status = Constants.ANSWER_RESPONSE_STATUS_NEGATIVE;
            output = e.getMessage();
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
}
