package org.commcare.formplayer.repo;

import org.commcare.formplayer.session.MenuSession;

/**
 * Created by willpride on 8/1/16.
 */
public class SerializableMenuSession {

    private String uuid;
    private String username;
    private String domain;
    private String appId;
    private String installReference;
    private String locale;
    private byte[] commcareSession;
    private String asUser;
    private boolean oneQuestionPerScreen;
    private boolean preview;

    public SerializableMenuSession(){}

    public SerializableMenuSession(MenuSession session) {
        this.uuid = session.getId();
        this.username = session.getUsername();
        this.domain = session.getDomain();
        this.appId = session.getAppId();
        this.installReference = session.getInstallReference();
        this.locale = session.getLocale();
        this.commcareSession = session.getCommcareSession();
        this.asUser = session.getAsUser();
        this.oneQuestionPerScreen = session.isOneQuestionPerScreen();
        this.preview = session.getPreview();

    }

    public SerializableMenuSession(String id, String username, String domain, String appId,
                                   String installReference, String locale, byte[] commcareSession,
                                   boolean oneQuestionPerScreen,
                                   String asUser, boolean preview){
        this.uuid = id;
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
