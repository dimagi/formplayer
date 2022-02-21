package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Bean used for evaluating xpath against a given menu session.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluateXPathMenuRequestBean extends SessionNavigationBean {
    private String xpath;
    private String debugOutput;

    // default constructor for Jackson
    public EvaluateXPathMenuRequestBean() {
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
        return "EvaluateXPathRequestBean [xpath: " + xpath +
                ", debugOutput: " + debugOutput + "]";
    }
}
