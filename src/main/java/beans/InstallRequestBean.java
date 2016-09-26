package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 2/4/16.
 */
public class InstallRequestBean extends AuthenticatedRequestBean implements AsUserBean {
    private String installReference;
    private String password;
    private String appId;
    private String locale;
    private String asUser;
    private boolean oneQuestionPerScreen;

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

    public String getAsUser() {
        return asUser;
    }

    public void setAsUser(String asUser) {
        this.asUser = asUser;
    }

    public boolean getOneQuestionPerScreen() {
        return oneQuestionPerScreen;
    }

    public void setOneQuestionPerScreen(boolean oneQuestionPerScreen) {
        this.oneQuestionPerScreen = oneQuestionPerScreen;
    }
}
