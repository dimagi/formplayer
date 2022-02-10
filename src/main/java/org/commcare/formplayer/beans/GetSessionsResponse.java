package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import org.commcare.formplayer.objects.FormSessionListView;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.cases.model.Case;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Response containing a list of the user's incomplete form sessions.
 * Requires the Case storage in order to lookup case names for display
 */
public class GetSessionsResponse {

    private long totalRecords;
    private SessionListItem[] sessions;

    public GetSessionsResponse() {
    }

    public GetSessionsResponse(SqlStorage<Case> caseStorage, ArrayList<FormSessionListView> sessionList, long totalRecords) {
        sessions = new SessionListItem[sessionList.size()];
        for (int i = 0; i < sessionList.size(); i++) {
            sessions[i] = new SessionListItem(caseStorage, sessionList.get(i));
        }
        this.totalRecords = totalRecords;
    }

    public SessionListItem[] getSessions() {
        return sessions;
    }

    public void setSessions(SessionListItem[] sessions) {
        this.sessions = sessions;
    }

    @JsonSetter(value = "total_records")
    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    @JsonGetter(value = "total_records")
    public long getTotalRecords() {
        return totalRecords;
    }

    @Override
    public String toString() {
        return "GetSessionsResponse sessions=" + Arrays.toString(sessions);
    }
}
