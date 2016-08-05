package util;

import beans.NewSessionRequestBean;

/**
 * Created by willpride on 2/4/16.
 */
public class StringUtils {
    public static String getFullUsername(String user, String domain, String host){
        return user + "@" + domain + "." + host;
    }

    public static String buildPostUrl(String host, NewSessionRequestBean newSessionBean){
        return String.format("%s/a/%s/receiver/%s/", host,
                newSessionBean.getSessionData().getDomain(),
                newSessionBean.getSessionData().getAppId());
    }
}
