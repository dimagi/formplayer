package hq.models;

import hq.interfaces.CouchUser;

import java.util.Arrays;

/**
 * A representation of HQ's CommCareUser. This is a mobile worker and can
 * only belong to one domain.
 */
public class CommCareUser implements CouchUser {
    private String domain;
    private String username;
    private boolean is_superuser;

    public CommCareUser() {}

    public boolean isAuthorized(String domain, String username) {
        if (is_superuser) {
            return true;
        }
        return this.domain.equals(domain) && this.username.equals(username);
    }
}
