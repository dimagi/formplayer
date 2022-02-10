package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 1/20/16. SessionResponseBean that evaluates the given XPath against the
 * current session evaluation context
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluateXPathRequestBean extends SessionRequestBean {
    private String xpath;
    private String debugOutput;

    // default constructor for Jackson
    public EvaluateXPathRequestBean() {
    }

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

    public String toString() {
        return "EvaluateXPathRequestBean [xpath: " + xpath + ", sessionId: " + sessionId +
                ", debugOutput: " + debugOutput + "]";
    }
}
