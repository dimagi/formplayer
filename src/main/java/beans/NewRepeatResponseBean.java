package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 1/20/16.
 */
public class NewRepeatResponseBean {
    private String tree;
    private String sequenceId;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public NewRepeatResponseBean(){}

    public NewRepeatResponseBean(String tree, String sequenceId) {
        this.tree = tree;
        this.sequenceId = sequenceId;
    }

    public String getTree() {
        return tree;
    }

    public void setTree(String tree) {
        this.tree = tree;
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
        return "NewRepeatResponseBean: [tree=" + tree + ", seq_id: " + sequenceId + "]";
    }
}
