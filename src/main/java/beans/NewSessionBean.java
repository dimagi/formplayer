package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import objects.SessionData;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by willpride on 1/20/16.
 */
@JsonIgnoreProperties
public class NewSessionBean {
    private String formUrl;
    private String lang;
    private Map<String, String> hqAuth;
    private SessionData sessionData;
    private Map<String, String> formContext;

    public NewSessionBean(){

    }

    public NewSessionBean(String formUrl, String lang, Map<String, String> hqAuth) {
        this.formUrl = formUrl;
        this.lang = lang;
        this.hqAuth = hqAuth;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
    @JsonGetter(value = "form-url")
    public String getFormUrl() {
        return formUrl;
    }
    @JsonSetter(value = "form-url")
    public void setFormUrl(String formUrl) {
        this.formUrl = formUrl;
    }
    @JsonGetter(value = "hq_auth")
    public Map<String, String> getHqAuth() {
        return hqAuth;
    }
    @JsonSetter(value = "hq_auth")
    public void setHqAuth(Map<String, String> hqAuth) {
        this.hqAuth = hqAuth;
    }
    @JsonGetter(value = "session-data")
    public SessionData getSessionData() {
        return sessionData;
    }
    @JsonSetter(value = "session-data")
    public void setSessionData(SessionData sessionData) {
        this.sessionData = sessionData;
    }
    @JsonGetter(value = "form_context")
    public Map<String, String> getFormContext() {
        return formContext;
    }
    @JsonSetter(value = "form_context")
    public void setFormContext(Map<String, String> formContext) {
        this.formContext = formContext;
    }
}
