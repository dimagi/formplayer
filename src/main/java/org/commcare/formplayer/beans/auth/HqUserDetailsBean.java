package org.commcare.formplayer.beans.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HqUserDetailsBean {
    private String[] domains;
    private int djangoUserId;
    private boolean isSuperUser;
    private String username;
    private String authToken;

    public HqUserDetailsBean() {
    }

    public HqUserDetailsBean(String[] domains, String username, boolean isSuperuser) {
        this.domains = domains;
        this.username = username;
        this.isSuperUser = isSuperuser;
    }

    public int getDjangoUserId() {
        return djangoUserId;
    }

    public void setDjangoUserId(int djangoUserId) {
        this.djangoUserId = djangoUserId;
    }

    public boolean isSuperUser() {
        return isSuperUser;
    }

    public void setSuperUser(boolean superUser) {
        isSuperUser = superUser;
    }

    public String getUsername() { return username; }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String[] getDomains() { return domains; }

    public void setDomains(String[] domains) { this.domains = domains; }

    public boolean isAuthorized(String domain, String username) {
        return isSuperUser || Arrays.asList(domains).contains(domain) && this.username.equals(username);
    }
}
