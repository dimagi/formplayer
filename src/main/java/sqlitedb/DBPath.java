package sqlitedb;

import java.io.File;

abstract class DBPath {
    abstract String getDatabasePath();
    abstract String getDatabaseName();

    String getDatabaseFile() {
        return getDatabasePath() + File.separator + getDatabaseName() + ".db";
    }
}
