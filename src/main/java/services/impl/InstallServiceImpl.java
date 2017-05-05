package services.impl;

import engine.FormplayerConfigEngine;
import exceptions.UnresolvedResourceRuntimeException;
import installers.FormplayerInstallerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.commcare.resources.model.UnresolvedResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import sandbox.SqlSandboxUtils;
import services.FormplayerStorageFactory;
import services.InstallService;

import java.io.File;

/**
 * The InstallService handles configuring the application,
 * either from a .ccz or .ccpr reference or existing dbs.
 * This can involve app download, install, and initialization of resources.
 */
public class InstallServiceImpl implements InstallService {

    @Autowired
    FormplayerStorageFactory storageFactory;

    @Autowired
    FormplayerInstallerFactory formplayerInstallerFactory;

    @Autowired
    ArchiveFileRoot formplayerArchiveFileRoot;

    private final Log log = LogFactory.getLog(InstallServiceImpl.class);

    @Override
    public FormplayerConfigEngine configureApplication(String reference) throws Exception {
        String dbFilePath = storageFactory.getDatabaseFile();
        log.info("Configuring application with reference " + reference + " and dbPath: " + dbFilePath + ".");
        System.out.println(" and storage factory " + storageFactory);
        try {
            File dbFolder = new File(dbFilePath);
            if(dbFolder.exists()) {
                // Try reusing old install, fail quietly
                try {
                    FormplayerConfigEngine engine = new FormplayerConfigEngine(storageFactory, formplayerInstallerFactory, formplayerArchiveFileRoot);
                    engine.initEnvironment();
                    return engine;
                } catch (Exception e) {
                    log.error("Got exception " + e + " while reinitializing at path " + dbFilePath);
                }
            }
            // Wipe out folder and attempt install
            storageFactory.closeConnection();
            SqlSandboxUtils.deleteDatabaseFolder(dbFilePath);
            if (!dbFolder.getParentFile().exists() && !dbFolder.getParentFile().mkdirs()) {
                throw new RuntimeException("Error instantiationing folder " + dbFolder);
            }
            FormplayerConfigEngine engine = new FormplayerConfigEngine(storageFactory, formplayerInstallerFactory, formplayerArchiveFileRoot);
            if (reference.endsWith(".ccpr")) {
                engine.initFromLocalFileResource(reference);
            } else {
                engine.initFromArchive(reference);
            }
            engine.initEnvironment();
            return engine;
        } catch (UnresolvedResourceException e) {
            log.error("Got exception " + e + " while installing reference " + reference + " at path " + dbFilePath);
            throw new UnresolvedResourceRuntimeException(e);
        } catch (Exception e) {
            log.error("Got exception " + e + " while installing reference " + reference + " at path " + dbFilePath);
            SqlSandboxUtils.deleteDatabaseFolder(dbFilePath);
            throw e;
        }
    }
}
