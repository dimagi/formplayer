package application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * This class represents properties for the SQLite databases
 */

@Component
public class SQLiteProperties {

    private static String dataDir;

    public static String getDataDir() {
        if (dataDir == null) {
            return String.format("dbs%s", File.separator);
        }
        return dataDir;
    }

    @Value("${sqlite.dataDir}")
    public void setDataDir(String dataDir) {
        this.dataDir = normalizeUnixStylePathReferences(dataDir);
    }

    private String normalizeUnixStylePathReferences(String input) {
        return input.replaceAll("\\/",String.format("\\%s", File.separator));
    }
}
