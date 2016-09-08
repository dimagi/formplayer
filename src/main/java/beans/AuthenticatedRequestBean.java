package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by benrudolph on 9/8/16.
 */
public class AuthenticatedRequestBean {
    private String domain;
    private String username;

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
