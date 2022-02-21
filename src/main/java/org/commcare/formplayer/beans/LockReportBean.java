package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Returns true if the server is up and healthy
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LockReportBean {

    boolean locked;
    int secondsLocked;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public LockReportBean() {
    }

    public LockReportBean(boolean locked, int secondsLocked) {
        this.locked = locked;
        this.secondsLocked = secondsLocked;
    }

    public boolean getLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setSecondsLocked(int secondsLocked) {
        this.secondsLocked = secondsLocked;
    }

    public long getSeckondsLocked() {
        return secondsLocked;
    }
}
