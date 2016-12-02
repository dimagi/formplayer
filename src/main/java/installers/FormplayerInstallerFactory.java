package installers;

import org.commcare.resources.model.ResourceInstaller;
import org.commcare.resources.model.installers.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import services.FormplayerStorageFactory;

/**
 * Created by willpride on 12/1/16.
 */
@Component
public class FormplayerInstallerFactory extends org.commcare.resources.model.InstallerFactory {

    @Autowired
    FormplayerStorageFactory storageFactory;

    public ResourceInstaller getProfileInstaller(boolean forceInstall) {
        return new FormplayerProfileInstaller(forceInstall, storageFactory);
    }

    @Override
    public ResourceInstaller getXFormInstaller() {
        return new FormplayerXFormInstaller(storageFactory);
    }

    public ResourceInstaller getUserRestoreInstaller() {
        return new FormplayerOfflineUserRestoreInstaller(storageFactory);
    }

    public ResourceInstaller getSuiteInstaller() {
        return new FormplayerSuiteInstaller(storageFactory);
    }

    public ResourceInstaller getLocaleFileInstaller(String locale) {
        return new LocaleFileInstaller(locale);
    }

    public ResourceInstaller getLoginImageInstaller() {
        return new LoginImageInstaller();
    }

    public ResourceInstaller getMediaInstaller(String path) {
        return new MediaInstaller();
    }
}
