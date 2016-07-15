package beans;

import auth.DjangoAuth;
import auth.HqAuth;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import services.RestoreService;

import java.util.Map;

/**
 * Created by willpride on 1/12/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncDbRequestBean {

    private HqAuth hqAuth;
    private String username;
    private RestoreService restoreService;
    private String domain;

    public SyncDbRequestBean(){}

    @JsonGetter(value = "hq_auth")
    public HqAuth getHqAuth() {
        return hqAuth;
    }
    @JsonSetter(value = "hq_auth")
    public void setHqAuth(HqAuth hqAuth) {
        this.hqAuth = hqAuth;
    }

    @JsonIgnore
    public String getRestoreXml(){
        return restoreService.getRestoreXml(domain, hqAuth);
    }

    public void setRestoreService(RestoreService restoreService) {
        this.restoreService = restoreService;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain(){
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
