package services;

import java.sql.Connection;

/**
 * Created by willpride on 3/9/17.
 */
public interface ConnectionHandler {
    Connection getConnection();
}
