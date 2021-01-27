package org.commcare.formplayer.beans;

import org.commcare.formplayer.beans.menus.BaseResponseBean;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 2/8/16.
 */
public class SessionResponseBean extends BaseResponseBean {
    String sessionId;
    int sequenceId;

    protected InstanceXmlBean instanceXml;

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

    @Override
    public String toString(){
        return "SessionResponseBean [sessionId=" + sessionId + ", seqId=" + sequenceId + "]";
    }

    public InstanceXmlBean getInstanceXml() {
        return instanceXml;
    }

    public void setInstanceXml(InstanceXmlBean instanceXml) {
        this.instanceXml = instanceXml;
    }
}
