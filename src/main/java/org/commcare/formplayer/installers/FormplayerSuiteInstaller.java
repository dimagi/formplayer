package org.commcare.formplayer.installers;

import org.commcare.resources.model.installers.SuiteInstaller;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;

/**
 * Created by willpride on 12/1/16.
 */
public class FormplayerSuiteInstaller extends SuiteInstaller {

    public FormplayerSuiteInstaller(){}

    @Override
    protected IStorageUtilityIndexed<Suite> storage(CommCarePlatform platform) {
        if (cacheStorage == null) {
            cacheStorage = platform.getStorageManager().getStorage(Suite.STORAGE_KEY);
        }
        return cacheStorage;
    }
}
