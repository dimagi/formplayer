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
    public SyncDbRequestBean(){}

    @Override
    public String getAsUser() {
        return username;
    }
}
