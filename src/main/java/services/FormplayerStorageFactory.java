package services;

import beans.InstallRequestBean;
import org.apache.commons.lang3.StringUtils;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import repo.SerializableMenuSession;
import util.ApplicationUtils;

/**
 * Created by willpride on 12/1/16.
 */
public class FormplayerStorageFactory implements IStorageIndexedFactory{

    private String username;
    private String domain;
    private String appId;
    private String dbPath;
    private String trimmedUsername;
    private ThreadLocal<IStorageIndexedFactory> wrappedFactory;

    public void configure(InstallRequestBean authenticatedRequestBean) {
        this.username = authenticatedRequestBean.getUsername();
        this.appId = authenticatedRequestBean.getAppId();
        this.domain = authenticatedRequestBean.getDomain();
        this.trimmedUsername = StringUtils.substringBefore(username, "@");
        this.dbPath = ApplicationUtils.getApplicationDBPath(domain, username, appId);

        wrappedFactory = new ThreadLocal<IStorageIndexedFactory>() {
            @Override
            protected IStorageIndexedFactory initialValue() {
                return new IStorageIndexedFactory() {
                    @Override
                    public IStorageUtilityIndexed newStorage(String name, Class type) {
                        return new SqliteIndexedStorageUtility(type, name, trimmedUsername, dbPath);
                    }
                };
            }
        };
    }

    public void configure(SerializableMenuSession session) {
        this.username = session.getUsername();
        this.appId = session.getAppId();
        this.domain = session.getDomain();
        this.trimmedUsername = StringUtils.substringBefore(username, "@");
        this.dbPath = ApplicationUtils.getApplicationDBPath(domain, username, appId);
    }

    @Override
    public IStorageUtilityIndexed newStorage(String name, Class type) {
        return wrappedFactory.get().newStorage(name, type);
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
}
