package org.commcare.formplayer.sqlitedb;

import java.io.File;

public abstract class DBPath {
    public abstract String getDatabaseName();
    public abstract String getDatabasePath();

    String getDatabaseFile() {
        return getDatabasePath() + File.separator + getDatabaseName() + ".db";
    }
}
