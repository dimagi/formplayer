package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.annotations.ApiModelProperty;

/**
 * Created by benrudolph on 9/8/16.
 */
public class SessionRequestBean extends AuthenticatedRequestBean {
    @ApiModelProperty(value = "The id of the form entry session", required = true)
    protected String sessionId;
    private boolean debuggerEnabled;

    @JsonGetter(value = "session_id")
    public String getSessionId() {
        return sessionId;
    }
    @JsonSetter(value = "session_id")
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString(){
        return "SessionRequestBean [sessionId=" + sessionId + "]";
    }

    public boolean isDebuggerEnabled() {
        return debuggerEnabled;
    }

    public void setDebuggerEnabled(boolean debuggerEnabled) {
        this.debuggerEnabled = debuggerEnabled;
    }
}
