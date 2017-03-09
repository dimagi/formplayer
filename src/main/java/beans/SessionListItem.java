package beans;

import hq.CaseAPIs;
import objects.SerializableFormSession;
import sandbox.SqliteIndexedStorageUtility;
import org.commcare.cases.model.Case;
import util.FormplayerDateUtils;

import java.util.NoSuchElementException;

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
        this.caseName = tryLoadCaseName(caseStorage, session);
    }

    private String tryLoadCaseName(SqliteIndexedStorageUtility<Case> caseStorage, SerializableFormSession session) {
        String caseId = session.getSessionData().get("case_id");
        if (caseId == null) {
            return null;
        }
        try {
            CaseBean caseBean = CaseAPIs.getFullCase(caseId, caseStorage);
            return (String) caseBean.getProperties().get("case_name");
        } catch (NoSuchElementException e) {
            // This handles the case where the case is no longer open in the database.
            // The form will crash on open, but I don't know if there's a more elegant but not-opaque way to handle
            return "Case with id " + caseId + "does not exist!";
        }
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
