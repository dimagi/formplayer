package org.commcare.formplayer.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * This class represents properties for the SQLite databases
 */

@Component
public class SQLiteProperties {

    private static String dataDir;
    private static String tempDataDir;

    public static String getDataDir() {
        if (dataDir == null) {
            return String.format("dbs%s", File.separator);
        }
        return dataDir;
    }

    public static String getTempDataDir() {
        if (tempDataDir == null) {
            return String.format("tmp_dbs%s", File.separator);
        }
        return tempDataDir;
    }

    @Value("${sqlite.dataDir}")
    public void setDataDir(String dataDir) {
        this.dataDir = normalizeUnixStylePathReferences(dataDir);
    }

    @Value("${sqlite.tmpDataDir}")
    public void setTempDataDir(String tempDataDir) {
        this.tempDataDir = normalizeUnixStylePathReferences(tempDataDir);
    }
    private String normalizeUnixStylePathReferences(String input) {
        return input.replaceAll("\\/",String.format("\\%s", File.separator));
    }
}
