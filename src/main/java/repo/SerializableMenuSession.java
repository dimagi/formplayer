package repo;

import session.MenuSession;

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

    public SerializableMenuSession(){}

    public SerializableMenuSession(MenuSession session) {
        this.uuid = session.getId();
        this.username = session.getUsername();
        this.domain = session.getDomain();
        this.appId = session.getAppId();
        this.installReference = session.getInstallReference();
        this.locale = session.getLocale();
        this.commcareSession = session.getCommcareSession();

    }

    public SerializableMenuSession(String id, String username, String domain, String appId,
                                   String installReference, String locale, byte[] commcareSession){
        this.uuid = id;
        this.username = username;
        this.domain = domain;
        this.appId = appId;
        this.installReference = installReference;
        this.locale = locale;
        this.commcareSession = commcareSession;
    }

    public String getId() {
        return uuid;
    }

    public void getId(String uuid) {
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
}
