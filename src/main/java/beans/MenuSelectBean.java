package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 2/5/16.
 */
public class MenuSelectBean {
    private String sessionId;
    private String selection;

    @JsonGetter(value = "session-id")
    public String getSessionId() {
        return sessionId;
    }

    @JsonSetter(value = "session-id")
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    @Override
    public String toString(){
        return "MenuSelectBean [sessionId=" + sessionId + ", selection=" + selection + "]";
    }
}
