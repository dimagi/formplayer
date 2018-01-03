package objects;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Map;

/**
 * Created by willpride on 1/19/16.
 */
@Entity
@Table(name = "formplayer_sessions")
public class SerializableFormSession implements Serializable{
    private String id;
    private String instanceXml;
    private String formXml;
    private String username;
    private String initLang;
    private int sequenceId;
    private Map<String, String> sessionData;
    private String domain;
    private String postUrl;
    private String menuSessionId;
    private String title;
    private String dateOpened;
    private boolean oneQuestionPerScreen;
    private String asUser;
    private String currentIndex = "0";
    private String appId;
    private Map<String, FunctionHandler[]> functionContext;

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
    public boolean equals(Object obj) {
        return obj instanceof SerializableFormSession && obj.hashCode() == hashCode();
    }

    @Override
    public String toString(){
        return "Session [id=" + id + ", sequence=" + sequenceId + ", username=" + username
                + " domain=" + domain + ", instance=" + instanceXml + "]";
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

    public String getMenuSessionId() {
        return menuSessionId;
    }

    public void setMenuSessionId(String menuSessionId) {
        this.menuSessionId = menuSessionId;
    }

    public String getDateOpened() {
        return dateOpened;
    }

    public void setDateOpened(String dateOpened) {
        this.dateOpened = dateOpened;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean getOneQuestionPerScreen() {
        return oneQuestionPerScreen;
    }

    public void setOneQuestionPerScreen(boolean oneQuestionPerScreen) {
        this.oneQuestionPerScreen = oneQuestionPerScreen;
    }

    public String getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(String currentIndex) {
        this.currentIndex = currentIndex;
    }

    public String getAsUser() {
        return asUser;
    }

    public void setAsUser(String asUser) {
        this.asUser = asUser;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Map<String, FunctionHandler[]> getFunctionContext() {
        return functionContext;
    }

    public void setFunctionContext(Map<String, FunctionHandler[]> functionContext) {
        this.functionContext = functionContext;
    }
}
