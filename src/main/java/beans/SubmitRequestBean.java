package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Map;

/**
 * Created by willpride on 1/20/16.
 *
 * TODO: Validate answers
 * TODO: Error handling
 * TODO: Process in SQLite DB
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmitRequestBean {
    private Map<String, String> formContext;
    private String sessionId;
    private boolean prevalidated;
    private Map<String, String> answers;

    public SubmitRequestBean(){

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

    public boolean isPrevalidated() {
        return prevalidated;
    }

    public void setPrevalidated(boolean prevalidated) {
        this.prevalidated = prevalidated;
    }

    public Map<String, String> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<String, String> answers) {
        this.answers = answers;
    }

    @Override
    public String toString(){
        return "Submit Request Bean [formContent: " + formContext + ", sessionId: " + sessionId +
                ", prevalidated=" + prevalidated + ", answers=" + answers + "]";
    }
}
