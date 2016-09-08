package beans;

import auth.HqAuth;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import services.RestoreService;

/**
 * Created by willpride on 1/12/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncDbRequestBean extends AuthenticatedRequestBean {

    private HqAuth hqAuth;
    private RestoreService restoreService;

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
}
