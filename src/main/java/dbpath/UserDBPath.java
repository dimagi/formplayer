package dbpath;

import org.sqlite.SQLiteConnection;
import sandbox.SqlSandboxUtils;
import util.ApplicationUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class UserDBPath implements DBPath {

    private String domain;
    private String username;
    private String asUsername;


    public UserDBPath(String domain, String username, String asUsername) {
        this.domain = domain;
        this.username = username;
        this.asUsername = asUsername;
    }

    @Override
    public Connection getConnection() throws SQLException {
        DataSource dataSource = SqlSandboxUtils.getDataSource(ApplicationUtils.getUserDBName(), getDatabasePath());
        return dataSource.getConnection();
    }

    @Override
    public String getDatabasePath() {
        return ApplicationUtils.getUserDBPath(domain, username, asUsername);
    }

    @Override
    public String getDatabaseFile() {
        return ApplicationUtils.getUserDBFile(domain, username, asUsername);
    }

    @Override
    public Boolean matchesConnection(SQLiteConnection sqLiteConnection) {
        return sqLiteConnection.url().contains(getDatabasePath());
    }
}
