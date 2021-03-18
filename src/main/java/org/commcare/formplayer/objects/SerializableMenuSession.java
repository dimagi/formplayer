package org.commcare.formplayer.objects;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * Created by willpride on 8/1/16.
 */

@Entity
@Table(name="menu_sessions")
public class SerializableMenuSession {

    @Id @GeneratedValue(generator="uuid")
    @GenericGenerator(name="uuid", strategy="org.hibernate.id.UUIDGenerator")
    private String id;

    private String username;

    private String domain;

    @Column(name="appid")
    private String appId;

    @Column(name="installreference")
    private String installReference;

    @Column(name="locale")
    private String locale;

    @Column(name="commcaresession")
    private byte[] commcareSession;

    @Column(name="asuser")
    private String asUser;

    @Transient
    private boolean oneQuestionPerScreen;

    private boolean preview;

    public SerializableMenuSession(){}

    public SerializableMenuSession(String username, String domain, String appId,
                                   String installReference, String locale, byte[] commcareSession,
                                   boolean oneQuestionPerScreen,
                                   String asUser, boolean preview){
        this.username = username;
        this.domain = domain;
        this.appId = appId;
        this.installReference = installReference;
        this.locale = locale;
        this.commcareSession = commcareSession;
        this.asUser = asUser;
        this.oneQuestionPerScreen = oneQuestionPerScreen;
        this.preview = preview;
    }

    public String getId() {
        return id;
    }

    public void setId(String uuid) {
        this.id = uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getInstallReference() {
        return installReference;
    }

    public void setInstallReference(String installReference) {
        this.installReference = installReference;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public byte[] getCommcareSession() {
        return commcareSession;
    }

    public void setCommcareSession(byte[] commcareSession) {
        this.commcareSession = commcareSession;
    }

    @Override
    public String toString(){
        return "SerializedMenuSesison id=" + id + ", username=" + username +", domain=" + domain
                + ", ref=" + installReference +", appId=" + appId;
    }

    public String getAsUser() {
        return asUser;
    }

    public void setAsUser(String asUser) {
        this.asUser = asUser;
    }

    public boolean getOneQuestionPerScreen() {
        return oneQuestionPerScreen;
    }

    public void setOneQuestionPerScreen(boolean oneQuestionPerScreen) {
        this.oneQuestionPerScreen = oneQuestionPerScreen;
    }

    public boolean getPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }
}
