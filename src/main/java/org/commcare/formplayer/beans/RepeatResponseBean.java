package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Arrays;

/**
 * Created by willpride on 1/20/16.
 */
public class RepeatResponseBean {
    private QuestionBean[] tree;
    private String sequenceId;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public RepeatResponseBean(){}

    public RepeatResponseBean(QuestionBean[] tree, String sequenceId) {
        this.tree = tree;
        this.sequenceId = sequenceId;
    }

    public QuestionBean[] getTree() {
        return tree;
    }

    public void setTree(QuestionBean[] tree) {
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
        return "RepeatResponseBean: [tree=" + Arrays.toString(tree) + ", seq_id: " + sequenceId + "]";
    }
}
