package beans;

import session.FormEntrySession;

import java.io.IOException;

/**
 * Created by willpride on 1/20/16.
 */
public class SubmitResponseBean extends SessionBean{
    private String output;
    private String status;

    // default constructor for Jackson
    public SubmitResponseBean(){}

    public SubmitResponseBean(FormEntrySession session) throws IOException {
        status = "success";
        output = session.getInstanceXml();
        sessionId = session.getSessionId();
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
        return "SubmitResponseBean: [sessionId=" + sessionId + ",output= " + output + "]";
    }
}
