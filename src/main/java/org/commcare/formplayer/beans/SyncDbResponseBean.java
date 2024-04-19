package org.commcare.formplayer.beans;

import org.commcare.formplayer.util.Constants;

/**
 * Created by willpride on 1/12/16.
 */
public class SyncDbResponseBean {
    private String status = Constants.ANSWER_RESPONSE_STATUS_POSITIVE;
    private boolean attemptRestore = true;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean getAttemptRestore() {
        return attemptRestore;
    }

    public void setAttemptRestore(boolean attemptRestore) {
        this.attemptRestore = attemptRestore;
    }
}
