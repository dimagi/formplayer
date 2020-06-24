package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 2/4/16.
 */
public class InstallRequestBean extends AuthenticatedRequestBean {
    private String installReference;
    private String password;
    private String appId;
    private String locale;
    private boolean oneQuestionPerScreen;
    private boolean preview;

    public String getInstallReference() {
        return installReference;
    }

    public void setInstallReference(String installReference) {
        this.installReference = installReference;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @JsonGetter(value = "app_id")
    public String getAppId() {
        return appId;
    }

    @JsonSetter(value = "app_id")
    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String toString() {
        return "InstallRequestBean: [installReference=" + installReference +
                ", username=" + username + ", domain=" + domain + ", appId=" + appId +
                ", oneQuestionPerScreen: " + oneQuestionPerScreen + "]";
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean getOneQuestionPerScreen() {
        return oneQuestionPerScreen;
    }

    public void setOneQuestionPerScreen(boolean oneQuestionPerScreen) {
        this.oneQuestionPerScreen = oneQuestionPerScreen;
    }

    public boolean getPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }
}
