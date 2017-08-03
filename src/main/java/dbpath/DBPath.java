package dbpath;

import org.sqlite.SQLiteConnection;

import java.sql.Connection;
import java.sql.SQLException;

public interface DBPath {
    Connection getConnection() throws SQLException;
    String getDatabasePath();
    Boolean matchesConnection(SQLiteConnection sqLiteConnection);
}
