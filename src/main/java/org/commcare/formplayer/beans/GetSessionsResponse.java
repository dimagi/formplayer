package org.commcare.formplayer.beans;

import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.cases.model.Case;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Response containing a list of the user's incomplete form sessions.
 * Requires the Case storage in order to lookup case names for display
 */
public class GetSessionsResponse {

    private SessionListItem[] sessions;

    public GetSessionsResponse(){}

    public GetSessionsResponse(SqlStorage<Case> caseStorage, ArrayList<SerializableFormSession> sessionList){
        sessions = new SessionListItem[sessionList.size()];
        for (int i = 0; i < sessionList.size(); i++){
            sessions[i] = new SessionListItem(caseStorage, sessionList.get(i));
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
