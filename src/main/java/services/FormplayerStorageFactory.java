package services;

import application.Application;
import beans.InstallRequestBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.springframework.stereotype.Component;
import org.sqlite.SQLiteConnection;
import sandbox.SqlSandboxUtils;
import sandbox.SqliteIndexedStorageUtility;
import util.ApplicationUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * FormPlayer's storage factory that negotiates between parsers/installers and the storage layer
 */
@Component
public class FormplayerStorageFactory implements IStorageIndexedFactory, ConnectionHandler {

    private String username;
    private String domain;
    private String appId;
    private String databasePath;
    private String asUsername;

    private Connection connection;

    private final Log log = LogFactory.getLog(FormplayerStorageFactory.class);

    public void configure(InstallRequestBean authenticatedRequestBean) {
        configure(authenticatedRequestBean.getUsername(),
                authenticatedRequestBean.getDomain(),
                authenticatedRequestBean.getAppId(),
                authenticatedRequestBean.getRestoreAs());
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
        this.databasePath = ApplicationUtils.getApplicationDBPath(domain, username, asUsername, appId);
        closeConnection();
    }

    @Override
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                DataSource dataSource = SqlSandboxUtils.getDataSource(ApplicationUtils.getApplicationDBName(), databasePath);
                connection = dataSource.getConnection();
            } else {
                if (connection instanceof SQLiteConnection) {
                    SQLiteConnection sqLiteConnection = (SQLiteConnection) connection;
                    if (!sqLiteConnection.url().contains(databasePath)) {
                        log.error(String.format("Had connection with path %s in StorageFactory %s",
                                sqLiteConnection.url(),
                                toString()));
                        DataSource dataSource = SqlSandboxUtils.getDataSource(ApplicationUtils.getApplicationDBName(), databasePath);
                        connection = dataSource.getConnection();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }
    
    public void closeConnection() {
        try {
            if(connection!= null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connection = null;
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

    public String getDatabaseFile() {
        return ApplicationUtils.getApplicationDBFile(domain, username, asUsername, appId);
    }

    public String getAsUsername() {
        return asUsername;
    }

    @Override
    public String toString() {
        return "FormplayerStorageFactory username=" + username + ", dbPath=" + databasePath +
                ", appId=" + appId + " connection=" + connection;
    }
}
