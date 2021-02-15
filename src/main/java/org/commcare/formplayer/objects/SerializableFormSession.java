package org.commcare.formplayer.objects;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Created by willpride on 1/19/16.
 */
@Entity
@Table(name = "formplayer_sessions")
@EntityListeners(AuditingEntityListener.class)
public class SerializableFormSession implements Serializable{
    @Id
    @GeneratedValue( generator = "uuid" )
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Version
    private int version;

    /**
     * Deprecated: to be removed once `dateCreated` is fully populated
     */
    @Column(name="dateopened", updatable=false)
    private String dateOpened;

    @CreatedDate
    @Column(name="datecreated")
    private Instant dateCreated;

    @Column(updatable=false)
    private String domain;

    @Column(name="asuser", updatable=false)
    private String asUser;

    @Column(name="appid", updatable=false)
    private String appId;

    @Column(name="caseid", updatable=false)
    private String restoreAsCaseId;

    @Column(name="posturl", updatable=false)
    private String postUrl;

    @Column(name="menu_session_id", updatable=false)
    private String menuSessionId;

    @Column(updatable=false)
    private String title;

    @Column(name="onequestionperscreen", updatable=false)
    private boolean oneQuestionPerScreen;

    @Column(name="formxml", updatable=false)
    private String formXml;

    @Column(name="instancexml")
    private String instanceXml;

    @Column(updatable=false)
    private String username;

    @Column(name="initlang")
    private String initLang;

    /**
     * Deprecated. To be replaced by ``version``
     */
    @Column(name="sequenceid")
    @Convert(converter=IntStringConverter.class)
    private Integer sequenceId;

    @Column(name="sessiondata")
    @Convert(converter=ByteArrayConverter.class)
    private Map<String, String> sessionData;

    @Column(name="currentindex")
    private String currentIndex;

    @Column(name="functioncontext")
    @Convert(converter=ByteArrayConverter.class)
    private Map<String, FunctionHandler[]> functionContext;

    @Column(name="inpromptmode")
    private boolean inPromptMode;

    public SerializableFormSession() { }
    public SerializableFormSession(String id) {
        this.id = id;
    }

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

    public boolean getInPromptMode() {
        return inPromptMode;
    }

    public void setInPromptMode(boolean inPromptMode) {
        this.inPromptMode = inPromptMode;
    }

    public void setRestoreAsCaseId(String restoreAsCaseId) {
        this.restoreAsCaseId = restoreAsCaseId;
    }

    public String getRestoreAsCaseId() {
        return restoreAsCaseId;
    }

    public void incrementSequence() {
        if (sequenceId == null) {
            sequenceId = 0;
        } else {
            sequenceId += 1;
        }
    }

    public Instant getDateCreated() {
        return dateCreated;
    }

    public int getVersion() {
        return version;
    }
}
