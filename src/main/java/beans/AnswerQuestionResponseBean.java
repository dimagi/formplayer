package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Map;

/**
 * Created by willpride on 1/20/16.
 */
public class AnswerQuestionResponseBean {
    private String tree;
    private String status;
    private String sequenceId;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public AnswerQuestionResponseBean(){}

    public AnswerQuestionResponseBean(String tree, String status, String sequenceId) {
        this.tree = tree;
        this.status = status;
        this.sequenceId = sequenceId;
    }

    public String getTree() {
        return tree;
    }

    public void setTree(String tree) {
        this.tree = tree;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    @JsonGetter(value = "seq_id")
    public String getSequenceId() {
        return sequenceId;
    }
    @JsonSetter(value = "seq_id")
    public void setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
    }

    @Override
    public String toString(){
        return "Answer Question Response Bean: [tree=" + tree + ", status=" + status + ", seq_id: " + sequenceId + "]";
    }
}
