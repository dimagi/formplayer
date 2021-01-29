package org.commcare.formplayer.beans.menus;

import java.util.Objects;

/**
 * Represents the validity status of a QuestionDef in a form
 */
public class ErrorBean {
    private String status;
    private String type;

    public ErrorBean() {}

    public ErrorBean(String status, String type) {
        this.status = status;
        this.type = type;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorBean errorBean = (ErrorBean) o;
        return Objects.equals(status, errorBean.status) && Objects.equals(type, errorBean.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, type);
    }
}
