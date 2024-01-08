package org.commcare.formplayer.beans;

/**
 * Useful meta data to include in request response
 */
public class ResponseMetaData {

    private boolean attemptRestore;
    private boolean appInstall;

    public ResponseMetaData() {
        // default constructor for jackson
    }

    public ResponseMetaData(boolean attemptRestore, boolean appInstall) {
        this.attemptRestore = attemptRestore;
        this.appInstall = appInstall;
    }

    public boolean isAttemptRestore() {
        return attemptRestore;
    }

    public boolean isAppInstall() {
        return appInstall;
    }
}
