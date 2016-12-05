package services;

import beans.InstallRequestBean;
import org.apache.commons.lang3.StringUtils;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.springframework.stereotype.Component;
import util.ApplicationUtils;

/**
 * FormPlayer's storage factory that negotiates between parsers/installers and the storage layer
 */
@Component
public class FormplayerStorageFactory implements IStorageIndexedFactory{

    private String username;
    private String domain;
    private String appId;
    private String databasePath;
    private String trimmedUsername;

    public void configure(InstallRequestBean authenticatedRequestBean) {
        this.username = authenticatedRequestBean.getUsername();
        this.appId = authenticatedRequestBean.getAppId();
        this.domain = authenticatedRequestBean.getDomain();
        this.trimmedUsername = StringUtils.substringBefore(username, "@");
        this.databasePath = ApplicationUtils.getApplicationDBPath(domain, username, appId);
    }

    public void configure(String databasePath, String trimmedUsername) {
        this.trimmedUsername = trimmedUsername;
        this.databasePath = databasePath;
    }

    @Override
    public IStorageUtilityIndexed newStorage(String name, Class type) {
        return new SqliteIndexedStorageUtility(type, trimmedUsername, name, databasePath);
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

    public String getDatabasePath() {
        return databasePath;
    }

    public String getTrimmedUsername() {
        return trimmedUsername;
    }
}
