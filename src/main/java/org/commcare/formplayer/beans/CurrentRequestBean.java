package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Map;

/**
 * Request to return the current question tree of the identified session
 *
 * Created by willpride on 1/20/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentRequestBean extends SessionRequestBean {
    private Map<String, String> formContext;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public CurrentRequestBean() {
    }

    @JsonGetter(value = "form_context")
    public Map<String, String> getFormContext() {
        return formContext;
    }

    @JsonSetter(value = "form_context")
    public void setFormContext(Map<String, String> formContext) {
        this.formContext = formContext;
    }

    @Override
    public String toString() {
        return "CurrentRequestBean [formContent=" + formContext + ", sessionId=" + sessionId + "]";
    }
}
