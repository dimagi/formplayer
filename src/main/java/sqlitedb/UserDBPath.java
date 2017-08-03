package sqlitedb;

import util.ApplicationUtils;

public class UserDBPath implements DBPath {

    private String domain;
    private String username;
    private String asUsername;


    public UserDBPath(String domain, String username, String asUsername) {
        this.domain = domain;
        this.username = username;
        this.asUsername = asUsername;
    }

    @Override
    public String getDatabasePath() {
        return ApplicationUtils.getUserDBPath(domain, username, asUsername);
    }

    @Override
    public String getDatabaseName() {
        return ApplicationUtils.getUserDBName();
    }

    @Override
    public String getDatabaseFile() {
        return ApplicationUtils.getUserDBFile(domain, username, asUsername);
    }
}
