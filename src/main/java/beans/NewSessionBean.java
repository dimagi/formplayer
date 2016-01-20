package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Map;

/**
 * Created by willpride on 1/20/16.
 */
public class NewSessionBean {
    private String formUrl;
    private String lang;
    private Map<String, String> hqAuth;

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
}
