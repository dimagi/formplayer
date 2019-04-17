package sqlitedb;

import java.io.File;

import util.Constants;

class ApplicationDBPath extends DBPath {

    private String domain;
    private String username;
    private String asUsername;
    private String appId;

    ApplicationDBPath(String domain, String username, String asUsername, String appId) {
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
