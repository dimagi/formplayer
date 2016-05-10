package objects;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by willpride on 1/19/16.
 */
public class SerializableFormSession implements Serializable{
    private String id;
    private String instanceXml;
    private String formXml;
    private String restoreXml;
    private String username;
    private String initLang;
    private int sequenceId;
    private Map<String, String> sessionData;
    private String domain;
    private String postUrl;

    public String getInstanceXml() {
        return instanceXml;
    }

    public void setInstanceXml(String instanceXml) {
        this.instanceXml = instanceXml;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int hashCode(){
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof SerializableFormSession){
            return obj.hashCode() == hashCode();
        }
        return false;
    }

    @Override
    public String toString(){
        return "Session [id=" + id + ", username=" + username
                + " domain=" + domain + ", instance=" + instanceXml + ", form=" + formXml +"]";
    }

    public String getFormXml() {
        return formXml;
    }

    public void setFormXml(String formXml) {
        this.formXml = formXml;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRestoreXml() {
        return restoreXml;
    }

    public void setRestoreXml(String restoreXml){
        this.restoreXml = restoreXml;
    }

    public String getInitLang() {
        return initLang;
    }

    public void setInitLang(String initLang) {
        this.initLang = initLang;
    }

    public int getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId;
    }

    public Map<String, String> getSessionData() {
        return sessionData;
    }

    public void setSessionData(Map<String, String> sessionData) {
        this.sessionData = sessionData;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }

    public String getPostUrl() {
        return postUrl;
    }
}
