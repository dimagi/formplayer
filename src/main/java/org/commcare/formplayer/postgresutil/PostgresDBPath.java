package org.commcare.formplayer.postgresutil;

import org.commcare.formplayer.sqlitedb.DBPath;
import org.commcare.formplayer.util.Constants;
import org.commcare.modern.database.TableBuilder;

/**
 * @author $|-|!˅@M
 */
class PostgresDBPath extends DBPath {

    private String domain;
    private String username;
    private String asUsername;
    private String appId;

    public PostgresDBPath(String domain, String username, String asUsername, String appId) {
        this.domain = domain;
        this.username = username;
        this.asUsername = asUsername;
        this.appId = appId;
    }

    @Override
    public String getDatabasePath() {
        String user = username.replace("@", "$");
        if (asUsername == null) {
            return TableBuilder.scrubName(user + "_" + appId);
        } else {
            return TableBuilder.scrubName(user + "_" + asUsername + "_" + appId);
        }
    }

    @Override
    public String getDatabaseName() {
        return "application_" + Constants.SQLITE_DB_VERSION;
    }
}
