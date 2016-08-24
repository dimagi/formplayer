package util;

import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;

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
        return UserSqlSandbox.DEFAULT_DATBASE_PATH + "/" + domain + "/" + username + "/" + appId;
    }
}
