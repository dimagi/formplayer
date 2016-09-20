package util;

import application.SQLiteProperties;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.modern.database.TableBuilder;

/**
 * Utility methods for dealing with Applications
 */
public class ApplicationUtils {

    public static boolean deleteApplicationDbs(String domain, String username, String appId) {
        Boolean success = true;

        try {
            SqlSandboxUtils.deleteDatabaseFolder(getApplicationDBPath(domain, username, appId));
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    public static String getApplicationDBPath(String domain, String username, String appId) {
        return SQLiteProperties.getDataDir() + domain + "/" + TableBuilder.scrubName(username) + "/" + appId;
    }
}
