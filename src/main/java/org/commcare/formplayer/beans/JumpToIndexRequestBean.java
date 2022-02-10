package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Move the current session to the selected index
 */
public class JumpToIndexRequestBean extends SessionRequestBean {
    private String formIndex;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public JumpToIndexRequestBean() {
    }

    public JumpToIndexRequestBean(String formIndex, String sessionId) {
        this.formIndex = formIndex;
        this.sessionId = sessionId;
    }

    @JsonGetter(value = "ix")
    public String getFormIndex() {
        return formIndex;
    }

    @JsonSetter(value = "ix")
    public void setFormIndex(String formIndex) {
        this.formIndex = formIndex;
    }

    @Override
    public String toString() {
        return "Questions for index nean [formIndex: " + formIndex + ", sessionId: " + sessionId + "]";
    }
}
