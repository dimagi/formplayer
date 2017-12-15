package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import session.MenuSession;

/**
 * Bean used for evaluating xpath against a given menu session.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluateXPathMenuRequestBean extends  InstallFromSessionRequestBean {
    private String xpath;
    private String debugOutput;

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


    @JsonGetter(value = "debugOutput")
    public String getDebugOutputLevel() {
        return debugOutput;
    }
    @JsonSetter(value = "debugOutput")
    public void setDebugOutputLevel(String debugOutput) {
        this.debugOutput = debugOutput;
    }

    public String toString(){
        return "EvaluateXPathRequestBean [xpath: " + xpath + ", menuSessionId: " + menuSessionId  +
                ", debugOutput: " + debugOutput + "]";
    }

    @JsonSetter(value = "session_id")
    public void setMenuSessionId(String menuSessionId) {
        this.menuSessionId = menuSessionId;
    }

    @JsonGetter(value = "session_id")
    public String getMenuSessionId() {
        return menuSessionId;
    }
}
