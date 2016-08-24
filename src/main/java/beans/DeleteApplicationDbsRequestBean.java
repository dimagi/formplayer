package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import static util.ApplicationUtils.deleteApplicationDbs;

/**
 * A request that delete's an application's databases
 */
public class DeleteApplicationDbsRequestBean {
    private String domain;
    private String username;
    private String appId;

    public DeleteApplicationDbsRequestBean(){}

    public boolean clear() {
        return deleteApplicationDbs(domain, username, appId);
    }

    @JsonGetter(value = "app_id")
    public String getAppId() {
        return appId;
    }
    @JsonSetter(value = "app_id")
    public void setAppId(String appId) {
        this.appId = appId;
    }

    @JsonGetter(value = "domain")
    public String getDomain() { return domain; }

    @JsonSetter(value = "domain")
    public void setDomain(String domain) { this.domain = domain; }

    @JsonSetter(value = "username")
    public String getUsername() { return username; }

    @JsonSetter(value = "username")
    public void setUsername(String username) { this.username = username; }
}
