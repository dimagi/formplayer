package beans;

import session.FormSession;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Response containing a list of the user's incomplete form sessions
 */
public class GetSessionsResponse {

    private SessionListItem[] sessions;

    public GetSessionsResponse(){}

    public GetSessionsResponse(ArrayList<FormSession> sessionList){
        sessions = new SessionListItem[sessionList.size()];
        for(int i = 0; i < sessionList.size(); i++){
            sessions[i] = new SessionListItem(sessionList.get(i));
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
