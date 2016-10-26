package beans;

import objects.SerializableFormSession;
import session.FormSession;

import java.util.Arrays;
import java.util.List;

/**
 * Response containing a list of the user's incomplete form sessions
 */
public class GetSessionsResponse {

    private SessionListItem[] sessions;

    public GetSessionsResponse(){}

    public GetSessionsResponse(FormSession[] sessionList){
        sessions = new SessionListItem[sessionList.length];
        for(int i = 0; i < sessionList.length; i++){
            sessions[i] = new SessionListItem(sessionList[i]);
        }
    }

    public SessionListItem[] getSessions() {
        return sessions;
    }

    public void setSessions(SessionListItem[] sessions) {
        this.sessions = sessions;
    }

    @Override
    public String toString(){
        return "GetSessionsResponse sessions=" + Arrays.toString(sessions);
    }
}
