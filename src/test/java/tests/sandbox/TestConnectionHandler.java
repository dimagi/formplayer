package tests.sandbox;

import sandbox.SqlSandboxUtils;
import services.ConnectionHandler;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by willpride on 3/13/17.
 */
public class TestConnectionHandler implements ConnectionHandler {

    private final String username;
    private final String dbPath;

    public TestConnectionHandler(String username, String dbPath) {
        this.dbPath = dbPath;
        this.username = username;
    }


    @Override
    public Connection getConnection() {
        try {
            return SqlSandboxUtils.getDataSource(username, dbPath).getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
