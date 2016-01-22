package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.json.JSONArray;
import session.FormEntrySession;

import java.io.IOException;

/**
 * Created by willpride on 1/20/16.
 */
public class SubmitResponseBean {
    private String output;
    private String status;
    private String sessionId;

    public SubmitResponseBean(){

    }

    public SubmitResponseBean(FormEntrySession session) throws IOException {
        status = "success";
        output = session.getInstanceXml();
        sessionId = session.getUUID();
    }


    @JsonGetter(value = "session_id")
    public String getSessionId() {
        return sessionId;
    }
    @JsonSetter(value = "session-id")
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString(){
        return "CurrentResponseBean: [sessionId=" + sessionId + "]";
    }
}
