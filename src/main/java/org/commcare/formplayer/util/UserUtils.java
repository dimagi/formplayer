package org.commcare.formplayer.util;

import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.javarosa.core.model.User;

import java.util.Iterator;

/**
 * Utility methods for dealing with users
 */
public class UserUtils {

    public static String getRestoreAsCaseIdUsername(String caseId) {
        return "CASE" + caseId;
    }

    // If given a domained username, return the un-domained username, otherwise return @param
    // username
    public static String getUsernameBeforeAtSymbol(String wrappedUsername) {
        return wrappedUsername.contains("@")
                ? wrappedUsername.substring(0, wrappedUsername.indexOf("@"))
                : wrappedUsername;
    }

    public static String getShortUsername(String username, String domain) {
        String usernameBeforeAtSymbol = getUsernameBeforeAtSymbol(username);
        String wouldBeFullUsername = StringUtils.getFullUsername(usernameBeforeAtSymbol, domain);
        if (wouldBeFullUsername.equals(username)) {
            return usernameBeforeAtSymbol;
        } else {
            return username;
        }
    }

    public static String getUserLocationsByDomain(String domain, UserSqlSandbox sandbox) {
        SqlStorage<User> userStorage = sandbox.getUserStorage();
        Iterator userIterator = userStorage.iterator();
        while (userIterator.hasNext()) {
            User uUser = (User)userIterator.next();
            String userDomain = uUser.getProperty("commcare_project");
            if (userDomain != null && userDomain.equals(domain)) {
                return uUser.getProperty("commcare_location_ids");
            }
        }
        return "";
    }
}
