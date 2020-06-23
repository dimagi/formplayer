package org.commcare.formplayer.sqlitedb;

public class ApplicationDB extends SQLiteDB {
    public ApplicationDB(String domain, String username, String asUsername, String appId) {
        super(new ApplicationDBPath(domain, username, asUsername, appId));
    }
}
