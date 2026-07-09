package org.commcare.formplayer.beans.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * HMAC-signed body sent to HQ's session_details endpoint for a public web apps session.
 *
 * Serializes to {@code {"publicSessionKey": ..., "domain": ...}}. HQ treats a request with a
 * truthy {@code publicSessionKey} as a public session lookup, in contrast to
 * {@link HqSessionKeyBean} which sends {@code sessionId}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HqPublicSessionKeyBean implements Serializable {
    private String publicSessionKey;
    private String domain;

    public HqPublicSessionKeyBean(String domain, String publicSessionKey) {
        this.domain = domain;
        this.publicSessionKey = publicSessionKey;
    }

    public String getPublicSessionKey() {
        return publicSessionKey;
    }

    public String getDomain() {
        return domain;
    }
}
