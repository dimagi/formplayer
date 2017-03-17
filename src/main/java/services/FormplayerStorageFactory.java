package services;

import beans.InstallRequestBean;
import org.apache.commons.lang3.StringUtils;
import org.javarosa.core.services.IPropertyManager;
import org.sqlite.SQLiteConnection;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;
import sandbox.SqlSandboxUtils;
import sandbox.SqliteIndexedStorageUtility;
import sandbox.UserSqlSandbox;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.springframework.stereotype.Component;
import util.ApplicationUtils;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * FormPlayer's storage factory that negotiates between parsers/installers and the storage layer
 */
@Component
public class FormplayerStorageFactory implements IStorageIndexedFactory, ConnectionHandler{

    private String username;
    private String domain;
    private String appId;
    private String databasePath;
    private String asUsername;

    private static final ThreadLocal<Connection> connection = new ThreadLocal<Connection>(){
        @Override
        protected Connection initialValue()
        {
            return null;
        }
    };

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
        this.databasePath = ApplicationUtils.getApplicationDBPath(domain, getUsernameDetail(), appId);
    }

    public String getUsernameDetail() {
        if (asUsername != null) {
            return username + "_" + asUsername;
        }
        return username;
    }

    @Override
    public Connection getConnection() {
        try {
            if (connection.get() == null || connection.get().isClosed()) {
                DataSource dataSource = SqlSandboxUtils.getDataSource("application", databasePath);
                connection.set(dataSource.getConnection());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return connection.get();
    }
    
    public static void closeConnection() {
        try {
            if(connection.get() != null && !connection.get().isClosed()) {
                connection.get().close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connection.set(null);
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
        return databasePath + "/application.db";
    }

    public String getAsUsername() {
        return asUsername;
    }

    @Override
    public String toString() {
        return "FormplayerStorageFactory username=" + username + ", dbPath=" + databasePath +
                ", appId=" + appId + " connection=" + connection.get();
    }
}
