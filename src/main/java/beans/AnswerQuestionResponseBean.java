package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by willpride on 1/20/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnswerQuestionResponseBean {
    private QuestionBean[] tree;
    private String status;
    private String sequenceId;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public AnswerQuestionResponseBean(){}

    public AnswerQuestionResponseBean(QuestionBean[] tree, String status, String sequenceId) {
        this.tree = tree;
        this.status = status;
        this.sequenceId = sequenceId;
    }

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
