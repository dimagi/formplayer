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

    public EvaluateXPathResponseBean(){

    }

    public EvaluateXPathResponseBean(FormEntrySession formEntrySession, String xpath) throws IOException, XPathSyntaxException {
        status = "success";
        EvaluationContext evaluationContext = formEntrySession.getFormEntryModel().getForm().getEvaluationContext();
        System.out.println("Evaluation XPath: " + xpath);
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
        return "CurrentResponseBean: [output=" + output + "]";
    }
}
