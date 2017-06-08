package util;

/**
 * Utility methods for dealing with users
 */
public class UserUtils {
    public static boolean isAnonymous(String domain, String username) {
        if (domain == null || username == null) {
            return false;
        }
        return username.equals(anonymousUsername(domain));
    }

    public static String anonymousUsername(String domain) {
        return Constants.ANONYMOUS_USERNAME + "@" + domain + "." + Constants.COMMCARE_USER_SUFFIX;
    }

    // If given a domained username, return the un-domained username, otherwise return @param username
    public static String unwrapUsername(String wrappedUsername) {
        return wrappedUsername.contains("@")
                ? wrappedUsername.substring(0, wrappedUsername.indexOf("@"))
                : wrappedUsername;
    }
}
