package util;

import application.SQLiteProperties;
import org.apache.tomcat.util.bcel.Const;
import sandbox.SqlSandboxUtils;
import org.commcare.modern.database.TableBuilder;

/**
 * Utility methods for dealing with Applications
 */
public class ApplicationUtils {

    public static boolean deleteApplicationDbs(String domain, String username, String asUsername, String appId) {
        Boolean success = true;

        try {
            SqlSandboxUtils.deleteDatabaseFolder(getApplicationDBPath(domain, username, asUsername, appId));
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    public static String getApplicationDBPath(String domain, String username, String asUsername, String appId) {
        return SQLiteProperties.getDataDir() + domain + "/" + TableBuilder.scrubName(getUsernameDetail(username, asUsername)) + "/" + appId;
    }

    public static String getApplicationDBFile(String domain, String username, String asUsername, String appId) {
        return getApplicationDBPath(domain, username, asUsername, appId) + "/" + getApplicationDBName() + ".db";
    }

    public static String getUserDBPath(String domain, String username, String asUsername) {
        return SQLiteProperties.getDataDir() + domain + "/" + TableBuilder.scrubName(getUsernameDetail(username, asUsername));
    }

    public static String getUserDBFile(String domain, String username, String asUsername) {
        return getUserDBPath(domain, username, asUsername) + "/" + getUserDBName() + ".db";
    }

    public static String getUserDBName() {
        return "user_" + Constants.SQLITE_DB_VERSION;
    }

    public static String getApplicationDBName() {
        return "application_" + Constants.SQLITE_DB_VERSION;
    }

    private static String getUsernameDetail(String username, String asUsername) {
        if (asUsername != null) {
            return username + "_" + asUsername;
        }
        return username;
    }
}
