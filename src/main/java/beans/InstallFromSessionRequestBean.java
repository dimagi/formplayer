package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 2/4/16.
 */
public class InstallFromSessionRequestBean extends AuthenticatedRequestBean {
    protected String menuSessionId;

    public String toString() {
        return "InstallFromSessionRequestBean: menuSessionId=" + menuSessionId + "]";
    }

    public String getMenuSessionId() {
        return menuSessionId;
    }

    public void setMenuSessionId(String menuSessionId) {
        this.menuSessionId = menuSessionId;
    }

}
