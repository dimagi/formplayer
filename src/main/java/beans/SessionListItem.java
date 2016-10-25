package beans;

import session.FormSession;
import util.FormplayerDateUtils;

/**
 * Individual display item in list of incomplete form sessions
 */
public class SessionListItem {

    private String title;
    private String dateOpened;
    private String sessionId;
    private String caseName;

    public SessionListItem(FormSession session){
        this.title = session.getTitle();
        this.dateOpened = FormplayerDateUtils.convertJavaDateStringToISO(session.getDateOpened());
        this.sessionId = session.getSessionId();
        this.caseName = session.getCaseName();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDateOpened() {
        return dateOpened;
    }

    public void setDateOpened(String dateOpened) {
        this.dateOpened = dateOpened;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCaseName() {
        return caseName;
    }
}
