package beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Arrays;

/**
 * Created by willpride on 1/20/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormEntryResponseBean extends SessionResponseBean{
    private QuestionBean[] tree;
    private String status;
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

    @Override
    public String toString(){
        return "FormEntryResponseBean: [status=" + status + ", seq_id: " + sequenceId
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
