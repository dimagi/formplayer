package sqlitedb;

import util.ApplicationUtils;
import util.Constants;

class ApplicationDBPath implements DBPath {

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
        return ApplicationUtils.getUserDBPath(domain, username, asUsername) + "/" + appId;
    }

    @Override
    public String getDatabaseName() {
        return "application_" + Constants.SQLITE_DB_VERSION;
    }

    @Override
    public String getDatabaseFile() {
        return getDatabasePath() + "/" + getDatabaseName() + ".db";
    }
}
