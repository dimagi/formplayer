package mocks;

import engine.FormplayerConfigEngine;
import installers.FormplayerInstallerFactory;
import sandbox.SqlSandboxUtils;
import org.springframework.beans.factory.annotation.Autowired;
import services.FormplayerStorageFactory;
import services.InstallService;

import java.io.File;
import java.net.URL;

/**
 * Created by willpride on 12/7/16.
 */
public class TestInstallService implements InstallService {

    @Autowired
    FormplayerStorageFactory storageFactory;

    @Autowired
    FormplayerInstallerFactory formplayerInstallerFactory;

    @Override
    public FormplayerConfigEngine configureApplication(String reference) {
        try {
            File dbFolder = new File(storageFactory.getDatabasePath());
            if(dbFolder.exists()) {
                // Try reusing old install, fail quietly
                try {
                    FormplayerConfigEngine engine = new FormplayerConfigEngine(storageFactory, formplayerInstallerFactory);
                    engine.initEnvironment();
                    return engine;
                } catch (Exception e) {
                    // pass
                }
            }
            SqlSandboxUtils.deleteDatabaseFolder(storageFactory.getDatabasePath());
            dbFolder.mkdirs();
            FormplayerConfigEngine engine = new FormplayerConfigEngine(storageFactory,
                    formplayerInstallerFactory);
            String absolutePath = getTestResourcePath(reference);
            engine.initFromArchive(absolutePath);
            engine.initEnvironment();
            return engine;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private String getTestResourcePath(String resourcePath){
        try {
            URL url = this.getClass().getClassLoader().getResource(resourcePath);
            File file = new File(url.getPath());
            return file.getAbsolutePath();
        } catch(NullPointerException npe){
            npe.printStackTrace();
            throw npe;
        }
    }
}
