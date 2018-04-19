package beans.menus;

import beans.InstallRequestBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Bean for requesting an update and specifying to what version
 */
public class UpdateRequestBean extends InstallRequestBean {

    private final Log log = LogFactory.getLog(UpdateRequestBean.class);

    private static final String[] VALID_UPDATE_MODES_ARRAY = new String[]{"build", "save", "release"};
    private static final Set<String> VALID_UPDATE_MODES = new HashSet<>(Arrays.asList(VALID_UPDATE_MODES_ARRAY));

    private String updateMode; // one of build, release, save

    private String sessionId;

    public String getUpdateMode() {
        // Default to 'save' since this is only accessible from App Preview
        return updateMode == null ? "save" : updateMode;
    }

    public void setUpdateMode (String updateMode) {
        this.updateMode = updateMode;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
