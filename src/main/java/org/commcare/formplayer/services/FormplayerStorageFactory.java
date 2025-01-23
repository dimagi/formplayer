package org.commcare.formplayer.services;

import datadog.trace.api.Trace;
import org.commcare.formplayer.beans.InstallRequestBean;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.modern.database.TableBuilder;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.Property;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sqlitedb.ApplicationDB;
import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.commcare.formplayer.util.FormplayerPropertyManager;
import org.commcare.formplayer.util.UserUtils;

/**
 * FormPlayer's storage factory that negotiates between parsers/installers and the storage layer
 */
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class FormplayerStorageFactory implements IStorageIndexedFactory {

    private String username;
    private String domain;
    private String appId;
    private String asUsername;

    private SQLiteDB sqLiteDB = new SQLiteDB(null);

    private FormplayerPropertyManager propertyManager;
    private StorageManager storageManager;

    @Autowired
    private FormSessionService formSessionService;

    public void configure(InstallRequestBean installRequestBean) {
        configure(
                installRequestBean.getUsername(),
                installRequestBean.getDomain(),
                installRequestBean.getAppId(),
                installRequestBean.getRestoreAs(),
                installRequestBean.getRestoreAsCaseId()
        );
    }

    public void configure(String formSessionId) {
        SerializableFormSession formSession = formSessionService.getSessionById(formSessionId);
        configure(formSession);
    }

    public void configure(SerializableFormSession formSession) {
        configure(formSession.getUsername(),
                formSession.getDomain(),
                formSession.getAppId(),
                formSession.getAsUser(),
                formSession.getRestoreAsCaseId()
        );
    }

    public void configure(String username, String domain, String appId, String asUsername, String restoreAsCaseId) {
        if (restoreAsCaseId != null) {
            configure(
                    UserUtils.getRestoreAsCaseIdUsername(restoreAsCaseId),
                    domain,
                    appId,
                    null
            );
        }
        else {
            configure(username, domain, appId, asUsername);
        }
    }

    @Trace
    public void configure(String username, String domain, String appId, String asUsername) {
        if(username == null || domain == null || appId == null) {
            throw new RuntimeException(String.format("Cannot configure FormplayerStorageFactory with null arguments. " +
                    "username = %s, domain = %s, appId = %s", username, domain, appId));
        }
        username = TableBuilder.scrubName(username);
        this.username = username;
        this.asUsername = asUsername;
        this.domain = domain;
        this.appId = appId;
        this.sqLiteDB = new ApplicationDB(domain, username, asUsername, appId);
        this.sqLiteDB.closeConnection();
        this.propertyManager = new FormplayerPropertyManager(newStorage(PropertyManager.STORAGE_KEY, Property.class));
        storageManager = new StorageManager(this);
    }

    @PreDestroy
    public void preDestroy() {
        if(sqLiteDB != null) {
            sqLiteDB.closeConnection();
        }
    }

    /**
     * Call after configuring factory if form def storage needs to be accessible.
     * Menu navigation requests setup form def storage as part of MenuSession initialization.
     * Form controller requests do not setup form def storage as part of FormSession initialization.
     */
    public void registerFormDefStorage() {
        getStorageManager().registerStorage(FormDef.STORAGE_KEY, FormDef.class);
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
