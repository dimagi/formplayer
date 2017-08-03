package services;

import beans.InstallRequestBean;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import repo.MenuSessionRepo;
import repo.SerializableMenuSession;
import sandbox.SqliteIndexedStorageUtility;
import sqlitedb.ApplicationDB;
import sqlitedb.SQLiteDB;

/**
 * FormPlayer's storage factory that negotiates between parsers/installers and the storage layer
 */
@Component
public class FormplayerStorageFactory implements IStorageIndexedFactory {

    private String username;
    private String domain;
    private String appId;
    private String asUsername;

    private SQLiteDB sqLiteDB = new SQLiteDB(null);

    @Autowired
    protected MenuSessionRepo menuSessionRepo;

    public void configure(InstallRequestBean installRequestBean) {
        configure(
                installRequestBean.getUsername(),
                installRequestBean.getDomain(),
                installRequestBean.getAppId(),
                installRequestBean.getRestoreAs()
        );
    }

    public void configure(String menuSessionId) {
        SerializableMenuSession menuSession = menuSessionRepo.findOneWrapped(menuSessionId);
        configure(
                menuSession.getUsername(),
                menuSession.getDomain(),
                menuSession.getAppId(),
                menuSession.getAsUser()
        );
    }

    public void configure(String username, String domain, String appId, String asUsername) {
        if(username == null || domain == null || appId == null) {
            throw new RuntimeException(String.format("Cannot configure FormplayerStorageFactory with null arguments. " +
                    "username = %s, domain = %s, appId = %s", username, domain, appId));
        }
        this.username = username;
        this.asUsername = asUsername;
        this.domain = domain;
        this.appId = appId;
        this.sqLiteDB = new ApplicationDB(domain, username, asUsername, appId);
        this.sqLiteDB.closeConnection();
    }

    @Override
    public IStorageUtilityIndexed newStorage(String name, Class type) {
        return new SqliteIndexedStorageUtility(this.sqLiteDB, type, name);
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

    public SQLiteDB getSQLiteDB() {
        return sqLiteDB;
    }

    public String getAsUsername() {
        return asUsername;
    }

}
