package org.commcare.formplayer.installers;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInstaller;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.CommCarePlatform;

/**
 * Base class for installers that do not require any action for:
 *  unstage, revert, rollback, upgrade, uninstall, cleanup
 */
public abstract class SimpleInstaller implements ResourceInstaller<CommCarePlatform> {
    public boolean requiresRuntimeInitialization() {
        return true;
    }

    public boolean unstage(Resource r, int newStatus, CommCarePlatform platform) {
        return true;
    }

    public boolean revert(Resource r, ResourceTable table, CommCarePlatform platform) {
        return true;
    }

    public int rollback(Resource r, CommCarePlatform platform) {
        return Resource.getCleanFlag(r.getStatus());
    }

    public boolean upgrade(Resource r, CommCarePlatform platform) throws UnresolvedResourceException {
        return true;
    }

    public boolean uninstall(Resource r, CommCarePlatform platform) throws UnresolvedResourceException {
        return true;
    }

    public void cleanup() {
    }
}
