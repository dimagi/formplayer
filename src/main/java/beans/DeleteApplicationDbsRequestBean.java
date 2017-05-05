package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import static util.ApplicationUtils.deleteApplicationDbs;

/**
 * A request that delete's an application's databases
 */
public class DeleteApplicationDbsRequestBean extends AuthenticatedRequestBean {
    private String appId;

    public DeleteApplicationDbsRequestBean() {
    }

    public boolean clear() {
        return deleteApplicationDbs(domain, username, restoreAs, appId);
    }

    @JsonGetter(value = "app_id")
    public String getAppId() {
        return appId;
    }

    @JsonSetter(value = "app_id")
    public void setAppId(String appId) {
        this.appId = appId;
    }

    @Override
    public String toString() {
        return "DeleteApplicationDbsRequestBean with appId=" + appId + ", parent " + super.toString();
    }

}
