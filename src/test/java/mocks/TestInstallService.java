package mocks;

import engine.FormplayerArchiveFileRoot;
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

    @Autowired
    FormplayerArchiveFileRoot formplayerArchiveFileRoot;

    @Override
    public FormplayerConfigEngine configureApplication(String reference) {
        try {
            File dbFile = new File(storageFactory.getDatabaseFile());
            if(dbFile.exists()) {
                // Try reusing old install, fail quietly
                try {
                    FormplayerConfigEngine engine = new FormplayerConfigEngine(storageFactory, formplayerInstallerFactory, formplayerArchiveFileRoot);
                    engine.initEnvironment();
                    return engine;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            SqlSandboxUtils.deleteDatabaseFolder(storageFactory.getDatabaseFile());
            dbFile.getParentFile().mkdirs();
            FormplayerConfigEngine engine = new FormplayerConfigEngine(storageFactory,
                    formplayerInstallerFactory, formplayerArchiveFileRoot);
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
