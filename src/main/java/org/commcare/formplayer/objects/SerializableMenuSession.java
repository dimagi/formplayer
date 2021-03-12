package org.commcare.formplayer.objects;

import org.commcare.formplayer.session.MenuSession;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.*;

/**
 * Created by willpride on 8/1/16.
 */

@Entity
@Table(name="menu_sessions")
@EntityListeners(AuditingEntityListener.class)
public class SerializableMenuSession {

    @Id @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "uuid")
    private String uuid;

    @Column(name="username")
    private String username;

    @Column(name="domain")
    private String domain;

    @Column(name="appId")
    private String appId;

    @Column(name="installReference")
    private String installReference;

    @Column(name="locale")
    private String locale;

    // Todo: Will this be large, should this be @Lob?
    @Column(name="commcareSession")
    private byte[] commcareSession;

    @Column(name="asUser")
    private String asUser;

    @Column(name="oneQuestionPerScreen")
    private boolean oneQuestionPerScreen;

    @Column(name="preview")
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
        return uuid;
    }

    public void setId(String uuid) {
        this.uuid = uuid;
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
        return "SerializedMenuSesison id=" + uuid + ", username=" + username +", domain=" + domain
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
