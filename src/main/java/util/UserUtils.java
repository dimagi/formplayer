package util;

/**
 * Created by benrudolph on 2/1/17.
 */
public class UserUtils {
    public static boolean isAnonymous(String domain, String username) {
        return username.equals(anonymousUsername(domain));
    }

    public static String anonymousUsername(String domain) {
        return Constants.ANONYMOUS_USERNAME + "@" + domain + "." + Constants.COMMCARE_USER_SUFFIX;
    }
}
