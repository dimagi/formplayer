package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Map;

/**
 * The AuthenticatedRequestBean should be used for requests that
 * need to be authenticated with HQ. This Bean will ensure the
 * necessary json values are present in the request.
 */
public class AuthenticatedRequestBean {

    protected String domain;
    protected String username;
    protected String restoreAs;
    protected boolean mustRestore;
    private boolean useLiveQuery;

    private String sessionId;
    private String caseId;

    private Map<String, String> hqAuth;

    @JsonGetter(value = "hq_auth")
    public Map<String, String> getHqAuth() {
        return hqAuth;
    }
    @JsonSetter(value = "hq_auth")
    public void setHqAuth(Map<String, String> hqAuth) {
        this.hqAuth = hqAuth;
    }

    private int timezoneOffsetMillis = -1;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getRestoreAs() {
        return restoreAs;
    }

    public void setRestoreAs(String restoreAs) {
        this.restoreAs = restoreAs;
    }

    public String getUsernameDetail() {
        if (restoreAs != null) {
            return username + "_" + restoreAs;
        }
        return username;
    }

    @Override
    public String toString() {
        return "Authenticated request bean wih username=" + username +
                ", domain=" + domain +
                ", auth=" + hqAuth +
                ", restoreAs=" + restoreAs;
    }

    public boolean isMustRestore() {
        return mustRestore;
    }

    public void setMustRestore(boolean mustRestore) {
        this.mustRestore = mustRestore;
    }

    public boolean getUseLiveQuery() {
        return useLiveQuery;
    }

    public void setUseLiveQuery(boolean useLiveQuery) {
        this.useLiveQuery = useLiveQuery;
    }

    @JsonGetter(value = "tz_offset_millis")
    public int getTzOffset() {
        return this.timezoneOffsetMillis;
    }

    @JsonSetter(value = "tz_offset_millis")
    public void setTzOffset(int offset) {
        this.timezoneOffsetMillis = offset;
    }

    @JsonGetter(value = "session-id")
    public String getSessionId() {
        return sessionId;
    }
    @JsonSetter(value = "session-id")
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @JsonGetter(value = "case_id")
    public String getCaseId() {
        return caseId;
    }
    @JsonSetter(value = "case_id")
    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }
}