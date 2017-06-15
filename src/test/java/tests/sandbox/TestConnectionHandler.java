package tests.sandbox;

import sandbox.SqlSandboxUtils;
import services.ConnectionHandler;
import util.ApplicationUtils;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by willpride on 3/13/17.
 */
public class TestConnectionHandler implements ConnectionHandler {

    private final String dbPath;

    public TestConnectionHandler(String dbPath) {
        this.dbPath = dbPath;
    }


    @Override
    public Connection getConnection() {
        try {
            return SqlSandboxUtils.getDataSource(ApplicationUtils.getUserDBName(), dbPath).getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
