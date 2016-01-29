package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 1/20/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepeatRequestBean {
    private String formIndex;
    private String sessionId;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public RepeatRequestBean(){}

    public RepeatRequestBean(String formIndex, String sessionId) {
        this.formIndex = formIndex;
        this.sessionId = sessionId;
    }

    @JsonGetter(value = "ix")
    public String getFormIndex() {
        return formIndex;
    }
    @JsonSetter(value = "ix")
    public void setFormIndex(String formIndex) {
        this.formIndex = formIndex;
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
    public String toString(){
        return "RepeatRequestBean [formIndex: " + formIndex + ", sessionId: " + sessionId + "]";
    }
}
