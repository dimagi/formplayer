package services.impl;

import dbpath.DBPathConnectionHandler;
import engine.FormplayerConfigEngine;
import exceptions.UnresolvedResourceRuntimeException;
import installers.FormplayerInstallerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.commcare.resources.model.UnresolvedResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import services.FormplayerStorageFactory;
import services.InstallService;

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
        DBPathConnectionHandler dbPathConnectionHandler = storageFactory.getDbPathConnectionHandler();
        log.info("Configuring application with reference " + reference +
                " and dbPath: " + dbPathConnectionHandler.getDatabaseFileForLoggingPurposes() + " \n" +
                "and storage factory \" + storageFactory");
        try {
            if(dbPathConnectionHandler.databaseFolderExists()) {
                // Try reusing old install, fail quietly
                try {
                    FormplayerConfigEngine engine = new FormplayerConfigEngine(storageFactory, formplayerInstallerFactory, formplayerArchiveFileRoot);
                    engine.initEnvironment();
                    return engine;
                } catch (Exception e) {
                    log.error("Got exception " + e + " while reinitializing at path " + dbPathConnectionHandler.getDatabaseFileForLoggingPurposes());
                }
            }
            // Wipe out folder and attempt install
            storageFactory.closeConnection();
            dbPathConnectionHandler.deleteDatabaseFolder();
            if (!dbPathConnectionHandler.databaseFolderExists() && !dbPathConnectionHandler.createDatabaseFolder()) {
                throw new RuntimeException("Error instantiationing folder " + dbPathConnectionHandler.getDatabaseFileForLoggingPurposes());
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
            log.error("Got exception " + e + " while installing reference " + reference + " at path " + dbPathConnectionHandler.getDatabaseFileForLoggingPurposes());
            throw new UnresolvedResourceRuntimeException(e);
        } catch (Exception e) {
            log.error("Got exception " + e + " while installing reference " + reference + " at path " + dbPathConnectionHandler.getDatabaseFileForLoggingPurposes());
            dbPathConnectionHandler.deleteDatabaseFolder();
            throw e;
        }
    }
}
