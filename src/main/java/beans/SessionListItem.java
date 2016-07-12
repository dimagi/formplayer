package beans;

import objects.SerializableFormSession;

/**
 * Individual display item in list of incomplete form sessions
 */
public class SessionListItem {

    private String sessionId;

    public SessionListItem(SerializableFormSession session){
        this.sessionId = session.getId();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
