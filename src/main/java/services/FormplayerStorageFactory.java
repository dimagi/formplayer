package services;

import beans.InstallRequestBean;
import dbpath.ApplicationDBPath;
import dbpath.DBPath;
import dbpath.DBPathConnectionHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import repo.MenuSessionRepo;
import repo.SerializableMenuSession;
import sandbox.SqliteIndexedStorageUtility;

import java.sql.Connection;

/**
 * FormPlayer's storage factory that negotiates between parsers/installers and the storage layer
 */
@Component
public class FormplayerStorageFactory implements IStorageIndexedFactory, ConnectionHandler {

    private String username;
    private String domain;
    private String appId;
    private String asUsername;

    private DBPath dbPath;
    private DBPathConnectionHandler dbPathConnectionHandler = new DBPathConnectionHandler(null, null);

    @Autowired
    protected MenuSessionRepo menuSessionRepo;

    private final Log log = LogFactory.getLog(FormplayerStorageFactory.class);

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
        this.dbPath = new ApplicationDBPath(domain, username, asUsername, appId);
        this.dbPathConnectionHandler = new DBPathConnectionHandler(dbPath, log);
        closeConnection();
    }

    @Override
    public Connection getConnection() {
        return dbPathConnectionHandler.getConnection();
    }

    public void closeConnection() {
        dbPathConnectionHandler.closeConnection();
    }

    @Override
    public IStorageUtilityIndexed newStorage(String name, Class type) {
        return new SqliteIndexedStorageUtility(this, type, name);
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

    public DBPathConnectionHandler getDbPathConnectionHandler() {
        return dbPathConnectionHandler;
    }

    public String getAsUsername() {
        return asUsername;
    }

}
