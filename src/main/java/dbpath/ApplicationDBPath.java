package dbpath;

import org.sqlite.SQLiteConnection;
import sandbox.SqlSandboxUtils;
import util.ApplicationUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ApplicationDBPath implements DBPath {

    private String databasePath;

    public ApplicationDBPath(String domain, String username, String asUsername, String appId) {
        this.databasePath = ApplicationUtils.getApplicationDBPath(domain, username, asUsername, appId);
    }

    @Override
    public Connection getConnection() throws SQLException {
        DataSource dataSource = SqlSandboxUtils.getDataSource(ApplicationUtils.getApplicationDBName(), databasePath);
        return dataSource.getConnection();
    }

    @Override
    public String getDatabasePath() {
        return databasePath;
    }

    @Override
    public Boolean matchesConnection(SQLiteConnection sqLiteConnection) {
        return sqLiteConnection.url().contains(databasePath);
    }
}
