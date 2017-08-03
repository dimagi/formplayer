package sqlitedb;

import util.ApplicationUtils;

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
        return ApplicationUtils.getApplicationDBPath(domain, username, asUsername, appId);
    }

    @Override
    public String getDatabaseName() {
        return ApplicationUtils.getApplicationDBName();
    }

    @Override
    public String getDatabaseFile() {
        return ApplicationUtils.getApplicationDBFile(domain, username, asUsername, appId);
    }
}
