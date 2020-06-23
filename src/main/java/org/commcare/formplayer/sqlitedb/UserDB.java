package org.commcare.formplayer.sqlitedb;

public class UserDB extends SQLiteDB {
    public UserDB(String domain, String username, String asUsername) {
        super(new UserDBPath(domain, username, asUsername));
    }
}
