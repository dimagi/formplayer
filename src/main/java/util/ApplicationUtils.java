package util;

import org.commcare.api.persistence.SqlSandboxUtils;

/**
 * Utility methods for dealing with Applications
 */
public class ApplicationUtils {

    public static Boolean deleteApplicationDbs(String appId) {
        String dbPath = "dbs/" + appId;
        Boolean success = true;

        try {
            SqlSandboxUtils.deleteDatabaseFolder(dbPath);
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }
}
