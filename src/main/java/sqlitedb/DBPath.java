package sqlitedb;

abstract class DBPath {
    abstract String getDatabasePath();
    abstract String getDatabaseName();

    String getDatabaseFile() {
        return getDatabasePath() + "/" + getDatabaseName() + ".db";
    }
}
