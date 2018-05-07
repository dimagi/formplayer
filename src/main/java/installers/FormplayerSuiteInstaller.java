package installers;

import org.commcare.resources.model.installers.SuiteInstaller;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import services.FormplayerStorageFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
