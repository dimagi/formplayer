package org.commcare.formplayer.beans.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HqSessionKeyBean implements Serializable {
    private String sessionId;
    private String domain;

    public HqSessionKeyBean(String domain, String sessionId) {
        this.domain = domain;
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getDomain() {
        return domain;
    }
}
