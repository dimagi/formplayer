package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import org.commcare.formplayer.sqlitedb.ApplicationDB;

/**
 * A request that delete's an application's databases
 */
public class DeleteApplicationDbsRequestBean extends AuthenticatedRequestBean {
    private String appId;

    public DeleteApplicationDbsRequestBean() {
    }

    public void clear() {
        new ApplicationDB(domain, username, restoreAs, appId).deleteDatabaseFolder();
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
        return "DeleteApplicationDbsRequestBean with appId=" + appId + ", parent "
                + super.toString();
    }

}
