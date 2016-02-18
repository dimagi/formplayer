package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.parser.XPathSyntaxException;
import session.FormEntrySession;

import java.io.IOException;
import java.util.Hashtable;

/**
 * Created by willpride on 1/20/16.
 */
public class EvaluateXPathResponseBean {
    private String output;
    private String status;

    //Jackson requires the default constructor
    public EvaluateXPathResponseBean(){}

    public EvaluateXPathResponseBean(FormEntrySession formEntrySession, String xpath) throws IOException, XPathSyntaxException {
        // TODO: don't always return success
        status = "accepted";
        EvaluationContext evaluationContext = formEntrySession.getFormEntryModel().getForm().getEvaluationContext();
        Text mText = Text.XPathText(xpath, new Hashtable<String, Text>());
        output = mText.evaluate(evaluationContext);
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
