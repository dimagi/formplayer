package org.commcare.formplayer.sqlitedb;

import org.commcare.formplayer.application.SQLiteProperties;
import org.commcare.modern.database.TableBuilder;

import java.io.File;

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

    static String getUserDBPath(String domain, String username, String asUsername) {
        return SQLiteProperties.getDataDir() + domain + File.separator + TableBuilder.scrubName(getUsernameDetail(username, asUsername));
    }

    private static String getUsernameDetail(String username, String asUsername) {
        if (asUsername != null) {
            return username + "_" + asUsername;
        }
        return username;
    }

    @Override
    public String getDatabasePath() {
        return getUserDBPath(domain, username, asUsername);
    }

    @Override
    public String getDatabaseName() {
        return "user_" + Constants.SQLITE_DB_VERSION;
    }
}
