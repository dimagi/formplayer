package org.commcare.formplayer.services;

import org.commcare.formplayer.util.FormplayerDatadog;
import org.commcare.formplayer.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ResponseMetaDataTracker {

    private boolean attemptRestore = false;

    private boolean newInstall = false;

    @Autowired
    private FormplayerDatadog datadog;

    public boolean isAttemptRestore() {
        return attemptRestore;
    }

    public void setAttemptRestore(boolean attemptRestore) {
        datadog.addRequestScopedTag(Constants.METADATA_TAG, "attempt_restore");
        this.attemptRestore = attemptRestore;
    }

    public boolean isNewInstall() {
        return newInstall;
    }

    public void setNewInstall(boolean newInstall) {
        datadog.addRequestScopedTag(Constants.METADATA_TAG, "app_install");
        this.newInstall = newInstall;
    }
}
