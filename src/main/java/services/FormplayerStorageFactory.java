package services;

import beans.InstallRequestBean;
import objects.SerializableFormSession;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.Property;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import repo.FormSessionRepo;
import sandbox.SqlStorage;
import sqlitedb.ApplicationDB;
import sqlitedb.SQLiteDB;
import util.FormplayerPropertyManager;

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

    private FormplayerPropertyManager propertyManager;
    private StorageManager storageManager;

    @Autowired
    protected FormSessionRepo formSessionRepo;

    public void configure(InstallRequestBean installRequestBean) {
        configure(
                installRequestBean.getUsername(),
                installRequestBean.getDomain(),
                installRequestBean.getAppId(),
                installRequestBean.getRestoreAs()
        );
    }

    public void configure(String formSessionId) {
        SerializableFormSession formSession = formSessionRepo.findOneWrapped(formSessionId);
        if (formSession.getRestoreAsCaseId() != null) {
            configure(
                    "CASE" + formSession.getRestoreAsCaseId(),
                    formSession.getDomain(),
                    formSession.getAppId(),
                    null
            );
        } else {
            configure(
                    formSession.getUsername(),
                    formSession.getDomain(),
                    formSession.getAppId(),
                    formSession.getAsUser()
            );
        }
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
        this.propertyManager = new FormplayerPropertyManager(newStorage(PropertyManager.STORAGE_KEY, Property.class));
        storageManager = new StorageManager(this);
    }

    public FormplayerPropertyManager getPropertyManager() {
        return propertyManager;
    }

    @Override
    public IStorageUtilityIndexed newStorage(String name, Class type) {
        return new SqlStorage(this.sqLiteDB, type, name);
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

    public StorageManager getStorageManager() {
        return storageManager;
    }
}
