package hq.models;

import hq.interfaces.CouchUser;

import java.util.Arrays;

/**
 * Created by benrudolph on 9/7/16.
 */
public class WebUser implements CouchUser {
    private String[] domains;
    private String username;
    private boolean is_superuser;

    public WebUser() {}

    public boolean isAuthorized(String domain, String username) {
        if (is_superuser) {
            return true;
        }
        return Arrays.asList(domains).contains(domain) && this.username.equals(username);
    }

}
