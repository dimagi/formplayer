package org.commcare.formplayer.beans;

import org.commcare.formplayer.beans.menus.ErrorBean;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by willpride on 1/20/16.
 */
public class SubmitResponseBean extends SessionResponseBean {
    private String status;
    private String submitResponseMessage;
    private Map<String, ErrorBean> errors = new HashMap<>();
    private Object nextScreen;

    // default constructor for Jackson
    public SubmitResponseBean() {
    }

    public SubmitResponseBean(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, ErrorBean> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, ErrorBean> errors) {
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "SubmitResponseBean, [status=" + status + ", errors: " + errors +
                ", next screen= " + nextScreen + "]";
    }

    public Object getNextScreen() {
        return nextScreen;
    }

    public void setNextScreen(Object nextScreen) {
        this.nextScreen = nextScreen;
    }

    public String getSubmitResponseMessage() {
        return submitResponseMessage;
    }

    public void setSubmitResponseMessage(String submitResponseMessage) {
        this.submitResponseMessage = submitResponseMessage;
    }
}
