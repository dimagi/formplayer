package beans;

import session.FormSession;

import java.io.IOException;

/**
 * Created by willpride on 1/20/16.
 */
public class SubmitResponseBean extends SessionBean{
    private String output;
    private String status;
    private String postUrl;

    // default constructor for Jackson
    public SubmitResponseBean(){}

    public SubmitResponseBean(FormSession session) throws IOException {
        status = "success";
        output = session.getInstanceXml();
        sessionId = session.getSessionId();
        postUrl = session.getPostUrl();
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

    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }
}
