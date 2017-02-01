package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 1/20/16.
 */
public class RunQARequestBean extends AuthenticatedRequestBean {
    private String qaPlan;
    private String appId;
    private String password;

    // default constructor for Jackson
    public RunQARequestBean(){}

    public String getQaPlan() {
        return qaPlan;
    }

    public void setQaPlan(String qaPlan) {
        this.qaPlan = qaPlan;
    }

    @JsonGetter(value = "app_id")
    public String getAppId() {
        return appId;
    }

    @JsonSetter(value = "app_id")
    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
