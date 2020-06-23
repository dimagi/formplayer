package org.commcare.formplayer.beans.menus;

/**
 * Represents the validity status of a QuestionDef in a form
 */
public class ErrorBean {
    private String status;
    private String type;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString(){
        return "ErrorBean: [status=" + status + ", type=" + type +"]";
    }
}
