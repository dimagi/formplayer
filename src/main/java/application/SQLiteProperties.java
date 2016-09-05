package application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * This class represents properties for the SQLite databases
 */

@Component
public class SQLiteProperties {

    private static String dataDir;

    public static String getDataDir() {
        if (dataDir == null) {
            return "dbs/";
        }
        return dataDir;
    }

    @Value("${sqlite.dataDir}")
    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }
}
