package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Request to open an incomplete form session (starts form entry)
 */
public class IncompleteSessionRequestBean extends AuthenticatedRequestBean {
    private String sessionId;

    public IncompleteSessionRequestBean (){}

    @Override
    @JsonGetter(value = "sessionId")
    public String getSessionId() {
        return sessionId;
    }

    @Override
    @JsonSetter(value = "sessionId")
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
