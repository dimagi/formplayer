package beans.debugger;

import beans.InstallFromSessionRequestBean;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by benrudolph on 6/15/17.
 */
public class MenuDebuggerRequestBean extends InstallFromSessionRequestBean {

    @Override
    @JsonGetter("session_id")
    public String getMenuSessionId() {
        return super.getMenuSessionId();
    }

    @Override
    @JsonSetter("session_id")
    public void setMenuSessionId(String menuSessionId) {
        this.menuSessionId = menuSessionId;
    }
}
