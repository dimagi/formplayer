package dbpath;

import org.sqlite.SQLiteConnection;
import sandbox.SqlSandboxUtils;
import util.ApplicationUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ApplicationDBPath implements DBPath {

    private String domain;
    private String username;
    private String asUsername;
    private String appId;

    public ApplicationDBPath(String domain, String username, String asUsername, String appId) {
        this.domain = domain;
        this.username = username;
        this.asUsername = asUsername;
        this.appId = appId;
    }

    @Override
    public Connection getConnection() throws SQLException {
        DataSource dataSource = SqlSandboxUtils.getDataSource(getDatabaseName(), getDatabasePath());
        return dataSource.getConnection();
    }

    @Override
    public String getDatabasePath() {
        return ApplicationUtils.getApplicationDBPath(domain, username, asUsername, appId);
    }

    @Override
    public String getDatabaseName() {
        return ApplicationUtils.getApplicationDBName();
    }

    @Override
    public String getDatabaseFile() {
        return ApplicationUtils.getApplicationDBFile(domain, username, asUsername, appId);
    }

    @Override
    public Boolean matchesConnection(SQLiteConnection sqLiteConnection) {
        return sqLiteConnection.url().contains(getDatabasePath());
    }
}
