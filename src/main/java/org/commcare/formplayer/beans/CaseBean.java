package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.commcare.cases.model.Case;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by willpride on 2/20/16.
 */
public class CaseBean {
    private HashMap properties;
    private String userId;
    private String caseId;
    private boolean closed;
    private Date lastModified;

    public CaseBean(){

    }

    public CaseBean(Case cCase){
        properties = new HashMap<>();
        setCaseId(cCase.getCaseId());
        setClosed(cCase.isClosed());
        setUserId(cCase.getUserId());
        for(Object key: cCase.getProperties().keySet()){
            properties.put(key.toString(), cCase.getProperties().get(key).toString());
        }

        for(String meta: cCase.getMetaDataFields()){
            Object metaField = cCase.getMetaData(meta);
            properties.put(meta, metaField);
        }

        properties.put("case_name", cCase.getName());
        properties.put("last_modified", cCase.getLastModified());
    }

    @JsonGetter(value="properties")
    public HashMap getProperties() {
        return properties;
    }
    @JsonSetter(value="properties")
    public void setProperties(HashMap properties) {
        this.properties = properties;
    }
    @JsonGetter(value="user_id")
    public String getUserId() {
        return userId;
    }
    @JsonSetter(value="user_id")
    private void setUserId(String userId) {
        this.userId = userId;
    }
    @JsonGetter(value="case_id")
    public String getCaseId() {
        return caseId;
    }
    @JsonSetter(value="case_id")
    private void setCaseId(String caseId) {
        this.caseId = caseId;
    }
    @JsonGetter(value="closed")
    public boolean getClosed() {
        return closed;
    }
    @JsonSetter(value="closed")
    private void setClosed(boolean closed) {
        this.closed = closed;
    }

    @Override
    public String toString(){
        return "CaseBean [caseId=" + caseId + ", userId=" + userId + ", properties=" + properties + "]";
    }

    @JsonGetter(value="date_modified")
    public Date getLastModified() {
        return lastModified;
    }
    @JsonSetter(value="date_modified")
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
}
