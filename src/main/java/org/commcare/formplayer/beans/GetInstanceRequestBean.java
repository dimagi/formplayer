package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 1/20/16.
 * Get the current instance of the specified session
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetInstanceRequestBean {

    private String sessionId;

    public GetInstanceRequestBean() {
    }

    @JsonGetter(value = "session-id")
    public String getSessionId() {
        return sessionId;
    }

    @JsonSetter(value = "session-id")
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }


    @Override
    public String toString() {
        return "GetInstanceRequestBean [sessionId=" + sessionId + "]";
    }
}
