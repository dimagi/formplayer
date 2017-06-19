package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by benrudolph on 2/4/16.
 *
 * Use this request bean to make requests that install from a menu session id
 * rather than by using steps.
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
