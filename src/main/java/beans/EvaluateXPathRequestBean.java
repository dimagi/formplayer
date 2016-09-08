package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Map;

/**
 * Created by willpride on 1/20/16.
 * SessionResponseBean that evaluates the given XPath against the current session evaluation context
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluateXPathRequestBean extends SessionRequestBean {
    private Map<String, Object> formContext;
    private String xpath;

    // default constructor for Jackson
    public EvaluateXPathRequestBean(){}

    @JsonGetter(value = "xpath")
    public String getXpath() {
        return xpath;
    }
    @JsonSetter(value = "xpath")
    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    @JsonGetter(value = "form_context")
    public Map<String, Object> getFormContext() {
        return formContext;
    }
    @JsonSetter(value = "form_context")
    public void setFormContext(Map<String, Object> formContext) {
        this.formContext = formContext;
    }

    @Override
    public String toString(){
        return "EvaluateXPathRequestBean [formcontext: " + formContext + ", xpath: " + xpath + ", sessionId: " + sessionId + "]";
    }
}
