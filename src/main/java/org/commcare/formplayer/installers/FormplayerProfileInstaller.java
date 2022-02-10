package org.commcare.formplayer.installers;

import org.commcare.resources.model.installers.ProfileInstaller;
import org.commcare.suite.model.Profile;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;

/**
 * Created by willpride on 12/1/16.
 */
public class FormplayerProfileInstaller extends ProfileInstaller {

    public FormplayerProfileInstaller() {
    }

    public FormplayerProfileInstaller(boolean forceInstall) {
        super(forceInstall);
    }

    @Override
    protected IStorageUtilityIndexed<Profile> storage(CommCarePlatform platform) {
        if (cacheStorage == null) {
            cacheStorage = platform.getStorageManager().getStorage(Profile.STORAGE_KEY);
        }
        return cacheStorage;
    }
}
