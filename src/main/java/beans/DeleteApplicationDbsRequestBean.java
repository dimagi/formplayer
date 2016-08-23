package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import static util.ApplicationUtils.deleteApplicationDbs;

/**
 * A request that delete's an application's databases
 */
public class DeleteApplicationDbsRequestBean {
    private String appId;

    public DeleteApplicationDbsRequestBean(){}

    public Boolean clear() {
        return deleteApplicationDbs(appId);
    }

    @JsonGetter(value = "app_id")
    public String getAppId() {
        return appId;
    }
    @JsonSetter(value = "app_id")
    public void setAppId(String appId) {
        this.appId = appId;
    }

}
