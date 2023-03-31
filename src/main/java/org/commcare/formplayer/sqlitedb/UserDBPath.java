package org.commcare.formplayer.sqlitedb;

import static org.commcare.formplayer.DbUtils.getDbPathForUser;

import org.commcare.formplayer.util.Constants;

class UserDBPath extends DBPath {

    private String domain;
    private String username;
    private String asUsername;


    UserDBPath(String domain, String username, String asUsername) {
        this.domain = domain;
        this.username = username;
        this.asUsername = asUsername;
    }

    @Override
    public String getDatabasePath() {
        return getDbPathForUser(domain, username, asUsername);
    }

    @Override
    public String getDatabaseName() {
        return "user_" + Constants.SQLITE_DB_VERSION;
    }
}
