package util;

/**
 * Created by willpride on 2/4/16.
 */
public class StringUtils {
    public static String getFullUsername(String user, String domain, String host){
        return user + "@" + domain + "." + host;
    }
}
