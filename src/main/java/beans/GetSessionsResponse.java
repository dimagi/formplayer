package beans;

import java.util.Arrays;

/**
 * Created by willpride on 6/15/16.
 */
public class GetSessionsResponse {
    private String[] sessionIds;

    public String[] getSessionIds() {
        return sessionIds;
    }

    public void setSessionIds(String[] sessionIds) {
        this.sessionIds = sessionIds;
    }

    @Override
    public String toString(){
        return "GetSessionsResponse sessionIds=" + Arrays.toString(sessionIds);
    }
}
