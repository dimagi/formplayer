package beans;

import objects.SerializableFormSession;

/**
 * Individual display item in list of incomplete form sessions
 */
public class SessionListItem {

    private String title;
    private String dateOpened;
    private String sessionId;

    public SessionListItem(SerializableFormSession session){
        this.title = session.getTitle();
        this.dateOpened = session.getDateOpened();
        this.sessionId = session.getId();
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
}
