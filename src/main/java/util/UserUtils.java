package util;

/**
 * Utility methods for dealing with users
 */
public class UserUtils {

    public static String getRestoreAsCaseIdUsername(String caseId) {
        return "CASE" + caseId;
    }

    // If given a domained username, return the un-domained username, otherwise return @param username
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
}
