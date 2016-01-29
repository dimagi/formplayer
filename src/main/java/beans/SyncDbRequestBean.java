package beans;

import auth.DjangoAuth;
import auth.HqAuth;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import objects.SessionData;
import services.RestoreService;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by willpride on 1/12/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncDbRequestBean {

    private Map<String, String> hqAuth;
    private String username;
    private RestoreService restoreService;
    private String domain;

    @JsonGetter(value = "hq_auth")
    public Map<String, String> getHqAuth() {
        return hqAuth;
    }
    @JsonSetter(value = "hq_auth")
    public void setHqAuth(Map<String, String> hqAuth) {
        this.hqAuth = hqAuth;
    }

    @JsonIgnore
    public String getRestoreXml(){
        HqAuth auth = new DjangoAuth(hqAuth.get("key"));
        return restoreService.getRestoreXml(domain, auth);
    }

    public void setRestoreService(RestoreService restoreService) {
        this.restoreService = restoreService;
    }

    public String getUsername() {
        return username;
    }

    // hacky as fuck. Get the username from the domained username
    public void setUsername(String domainedUsername) {
        this.username = domainedUsername;
        String[] atSplit = domainedUsername.split("@");
        String domainedName = atSplit[1];
        String[] dotSplit = domainedName.split("\\.");
        domain = dotSplit[0];
    }

    public String getDomain(){
        return domain;
    }
}
