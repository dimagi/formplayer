package hq.models;

import hq.interfaces.CouchUser;

import java.util.Arrays;

/**
 * This is a representation of HQ's WebUser model. A web user can belong
 * to multiple domains and can also be a super user.
 */
public class WebUser implements CouchUser {
    private String[] domains;
    private String username;
    private boolean is_superuser;

    public WebUser() {}

    public WebUser(String[] domains, String username, boolean is_superuser) {
        this.domains = domains;
        this.username = username;
        this.is_superuser = is_superuser;
    }

    public boolean isAuthorized(String domain, String username) {
        if (is_superuser) {
            return true;
        }
        return Arrays.asList(domains).contains(domain) && this.username.equals(username);
    }

    public String getUsername() {
        return username;
    }

}
