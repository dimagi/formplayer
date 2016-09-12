package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * The AuthenticatedRequestBean should be used for requests that
 * need to be authenticated with HQ. This Bean will ensure the
 * necessary json values are present in the request.
 */
public class AuthenticatedRequestBean {
    protected String domain;
    protected String username;

    @JsonGetter(value = "username")
    public String getUsername() {
        return username;
    }

    @JsonSetter(value = "username")
    public void setUsername(String username) {
        this.username = username;
    }

    @JsonGetter(value = "domain")
    public String getDomain() {
        return domain;
    }

    @JsonSetter(value = "domain")
    public void setDomain(String domain) {
        this.domain = domain;
    }
}
