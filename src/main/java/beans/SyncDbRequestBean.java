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

    public void setUsername(String username) {
        System.out.println("Set Username: " + username);
        this.username = username;
        String[] atSplit = username.split("@");
        System.out.println("At split: " + Arrays.toString(atSplit));
        String domainedName = atSplit[1];
        System.out.println("Domained name: " +domainedName);
        String[] dotSplit = domainedName.split("\\.");
        System.out.println("dot split: " + Arrays.toString(dotSplit));
        domain = dotSplit[0];
    }

    public String getDomain(){
        return domain;
    }
}
