package beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Request to submit the form with the given answers
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmitRequestBean extends SessionRequestBean {
    private boolean prevalidated;
    private Map<String, Object> answers;

    public SubmitRequestBean(){

    }

    public boolean isPrevalidated() {
        return prevalidated;
    }

    public void setPrevalidated(boolean prevalidated) {
        this.prevalidated = prevalidated;
    }

    public Map<String, Object> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<String, Object> answers) {
        this.answers = answers;
    }

    @Override
    public String toString(){
        return "Submit Request Bean [sessionId: " + sessionId +
                ", prevalidated=" + prevalidated + ", answers=" + answers + "]";
    }
}
