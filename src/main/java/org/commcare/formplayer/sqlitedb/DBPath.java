package org.commcare.formplayer.sqlitedb;

import java.io.File;

public abstract class DBPath {
    abstract String getDatabasePath();
    abstract String getDatabaseName();

    String getDatabaseFile() {
        return getDatabasePath() + File.separator + getDatabaseName() + ".db";
    }
}
