package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 2/8/16.
 */
public class SessionBean {
    protected String sessionId;
    protected int sequenceId;

    @JsonGetter(value = "session_id")
    public String getSessionId() {
        return sessionId;
    }
    @JsonSetter(value = "session_id")
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @JsonGetter(value = "seq_id")
    public int getSequenceId() {
        return sequenceId;
    }
    @JsonSetter(value = "seq_id")
    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId;
    }
}
