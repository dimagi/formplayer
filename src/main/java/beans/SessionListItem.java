package beans;

import objects.SerializableFormSession;
import org.commcare.cases.model.Case;
import sandbox.SqliteIndexedStorageUtility;
import util.FormplayerDateUtils;
import util.SessionUtils;

/**
 * Individual display item in list of incomplete form sessions
 */
public class SessionListItem {

    private String title;
    private String dateOpened;
    private String sessionId;
    private String caseName;

    public SessionListItem(SqliteIndexedStorageUtility<Case> caseStorage, SerializableFormSession session){
        this.title = session.getTitle();
        this.dateOpened = FormplayerDateUtils.convertJavaDateStringToISO(session.getDateOpened());
        this.sessionId = session.getId();
        this.caseName = SessionUtils.tryLoadCaseName(caseStorage, session);
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

    @Override
    public String toString() {
        return "SessionListItem [title = " + title + " id " + sessionId + " opened " + dateOpened + "]";
    }
}
