package beans;

import auth.HqAuth;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 1/12/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncDbRequestBean extends AuthenticatedRequestBean implements AsUserBean {

    String asUser;

    public SyncDbRequestBean(){}

    @Override
    @JsonGetter(value = "restoreAs")
    public String getAsUser() {
        return asUser;
    }
    @JsonSetter(value = "restoreAs")
    public void setAsUser(String asUser) {
        this.asUser = asUser;
    }
}
