package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import objects.SessionData;

import java.util.Map;

/**
 * Created by willpride on 1/12/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseFilterRequestBean extends AuthenticatedRequestBean {

    private String filterExpression;
    private Map<String, String> hqAuth;
    private SessionData sessionData;

    @JsonGetter(value = "filter_expr")
    public String getFilterExpression() {
        return filterExpression;
    }
    @JsonSetter(value = "filter_expr")
    public void setFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
    }
    @JsonGetter(value = "hq_auth")
    public Map<String, String> getHqAuth() {
        return hqAuth;
    }
    @JsonSetter(value = "hq_auth")
    public void setHqAuth(Map<String, String> hqAuth) {
        this.hqAuth = hqAuth;
    }
    @JsonGetter(value = "session_data")
    public SessionData getSessionData() {
        return sessionData;
    }
    @JsonSetter(value = "session_data")
    public void setSessionData(SessionData sessionData) {
        this.sessionData = sessionData;
    }
}
