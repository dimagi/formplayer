package org.commcare.formplayer.sqlitedb;

import java.io.File;

import org.commcare.formplayer.util.Constants;

public class ApplicationDBPath extends DBPath {

    private String domain;
    private String username;
    private String asUsername;
    private String appId;

    public ApplicationDBPath(String domain, String username, String asUsername, String appId) {
        this.domain = domain;
        this.username = username;
        this.asUsername = asUsername;
        this.appId = appId;
    }

    @Override
    public String getDatabasePath() {
        return UserDBPath.getUserDBPath(domain, username, asUsername) + File.separator + appId;
    }

    @Override
    public String getDatabaseName() {
        return "application_" + Constants.SQLITE_DB_VERSION;
    }
}
