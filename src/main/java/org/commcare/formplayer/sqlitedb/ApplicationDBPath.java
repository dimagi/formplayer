package org.commcare.formplayer.sqlitedb;

import org.commcare.formplayer.DbUtils;
import org.commcare.formplayer.util.Constants;

import java.io.File;

class ApplicationDBPath extends DBPath {

    private String domain;
    private String username;
    private String asUsername;
    private String appId;
    private String appVersion;

    ApplicationDBPath(String domain, String username, String asUsername, String appId, String appVersion) {
        this.domain = domain;
        this.username = username;
        this.asUsername = asUsername;
        this.appId = appId;
        this.appVersion = appVersion;
    }

    @Override
    public String getDatabasePath() {
        String path = DbUtils.getDbPathForUser(domain, username, asUsername) + File.separator + appId;
        // Conditional check is for backwards compatability. Once HQ changes are deployed to include
        // app version in URL, this can be removed.
        if (appVersion != null) {
            path = path + "_" + appVersion;
        }
        return path;
    }

    @Override
    public String getDatabaseName() {
        return "application_" + Constants.SQLITE_DB_VERSION;
    }
}
