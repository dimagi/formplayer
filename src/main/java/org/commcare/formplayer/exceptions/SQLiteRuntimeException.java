package org.commcare.formplayer.exceptions;

import java.sql.SQLException;

/**
 * Created by willpride on 10/5/17.
 */
public class SQLiteRuntimeException extends RuntimeException {
    public SQLiteRuntimeException(SQLException e) {
        super(e);
    }
}
