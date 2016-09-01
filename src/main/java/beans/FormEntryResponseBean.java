package beans;

import beans.menus.BaseResponseBean;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Arrays;

/**
 * Created by willpride on 1/20/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormEntryResponseBean extends BaseResponseBean{
    private QuestionBean[] tree;
    private String status;
    private int sequenceId;
    private String reason;
    private String type;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public FormEntryResponseBean(){}

    public QuestionBean[] getTree() {
        return tree;
    }

    public void setTree(QuestionBean[] tree) {
        this.tree = tree;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        return "FormEntryResponseBean: [tree=" + Arrays.toString(tree) + ", status=" + status + ", seq_id: " + sequenceId
                + ( status != null && status.equals("error") ? ", reason=" + reason + ", type=" + type : "") +  "]";
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
