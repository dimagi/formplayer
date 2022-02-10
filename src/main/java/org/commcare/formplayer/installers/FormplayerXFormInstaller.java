package org.commcare.formplayer.installers;

import org.commcare.resources.model.installers.XFormInstaller;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;

/**
 * Created by willpride on 12/1/16.
 */
public class FormplayerXFormInstaller extends XFormInstaller {

    public FormplayerXFormInstaller() {
    }

    @Override
    protected IStorageUtilityIndexed<FormDef> storage(CommCarePlatform platform) {
        if (cacheStorage == null) {
            cacheStorage = platform.getStorageManager().getStorage(FormDef.STORAGE_KEY);
        }
        return cacheStorage;
    }
}
