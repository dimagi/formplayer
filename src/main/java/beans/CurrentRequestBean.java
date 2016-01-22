package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Map;

/**
 * Created by willpride on 1/20/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentRequestBean {
    private Map<String, String> formContext;
    private String sessionId;

    public CurrentRequestBean(){

    }

    @JsonGetter(value = "session-id")
    public String getSessionId() {
        return sessionId;
    }
    @JsonSetter(value = "session-id")
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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
    public String toString(){
        return "Answer Question Bean [formContente: " + formContext + ", sessionId: " + sessionId + "]";
    }
}
