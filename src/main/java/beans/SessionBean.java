package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Created by willpride on 2/8/16.
 */
@ApiModel("Answer Question Request")
public class SessionBean {
    @ApiModelProperty(value = "The id of the form entry session", required = true)
    String sessionId;
    @ApiModelProperty(value = "The sequence number of the current command", required = true)
    int sequenceId;

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
        return "SessionBean [sessionId=" + sessionId + ", seqId=" + sequenceId + "]";
    }

}
