package util;

import application.SQLiteProperties;
import org.commcare.modern.database.TableBuilder;
import sqlitedb.UserDB;

/**
 * Utility methods for dealing with Applications
 */
public class ApplicationUtils {


    public static void clearUserData(String domain, String username, String asUsername) {
        new UserDB(domain, username, asUsername).deleteDatabaseFolder();
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

    private static String getUsernameDetail(String username, String asUsername) {
        if (asUsername != null) {
            return username + "_" + asUsername;
        }
        return username;
    }
}
