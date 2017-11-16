package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import session.MenuSession;

/**
 * Bean used for evaluating xpath against a given menu session.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluateXPathMenuRequestBean extends SessionNavigationBean {
    private String xpath;

    // default constructor for Jackson
    public EvaluateXPathMenuRequestBean(){}

    @JsonGetter(value = "xpath")
    public String getXpath() {
        return xpath;
    }
    @JsonSetter(value = "xpath")
    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public String toString(){
        return "EvaluateXPathRequestBean [xpath: " + xpath + "]";
    }
}
