package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.commcare.formplayer.sqlitedb.ApplicationDB;

/**
 * A request that delete's an application's databases
 */
public class DeleteApplicationDbsRequestBean extends AuthenticatedRequestBean {
    private String appId;
    private String appVersion;

    public DeleteApplicationDbsRequestBean() {
    }

    public void clear() {
        new ApplicationDB(domain, username, restoreAs, appId, appVersion).deleteDatabaseFolder();
    }

    @JsonGetter(value = "app_id")
    public String getAppId() {
        return appId;
    }

    @JsonSetter(value = "app_id")
    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    @Override
    public String toString() {
        return "DeleteApplicationDbsRequestBean with appId=" + appId + ", appVersion=" + appVersion + ", parent " + super.toString();
    }

}
