package org.commcare.formplayer.services;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ResponseMetaDataTracker {

    private boolean attemptRestore = false;

    private boolean newInstall = false;

    public boolean isAttemptRestore() {
        return attemptRestore;
    }

    public void setAttemptRestore(boolean attemptRestore) {
        this.attemptRestore = attemptRestore;
    }

    public boolean isNewInstall() {
        return newInstall;
    }

    public void setNewInstall(boolean newInstall) {
        this.newInstall = newInstall;
    }
}
