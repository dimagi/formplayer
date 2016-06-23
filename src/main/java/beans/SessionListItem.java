package beans;

import objects.SerializableFormSession;

/**
 * Created by willpride on 6/23/16.
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
