package services;

import engine.FormplayerConfigEngine;
import exceptions.UnresolvedResourceRuntimeException;
import installers.FormplayerInstallerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.commcare.resources.model.UnresolvedResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sqlitedb.SQLiteDB;

/**
 * The InstallService handles configuring the application,
 * either from a .ccz or .ccpr reference or existing dbs.
 * This can involve app download, install, and initialization of resources.
 */
@Service
public class InstallService {

    @Autowired
    FormplayerStorageFactory storageFactory;

    @Autowired
    FormplayerInstallerFactory formplayerInstallerFactory;

    @Autowired
    ArchiveFileRoot formplayerArchiveFileRoot;

    private final Log log = LogFactory.getLog(InstallService.class);

    public FormplayerConfigEngine configureApplication(String reference) throws Exception {
        SQLiteDB sqliteDB = storageFactory.getSQLiteDB();
        log.info("Configuring application with reference " + reference +
                " and dbPath: " + sqliteDB.getDatabaseFileForDebugPurposes() + " \n" +
                "and storage factory \" + storageFactory");
        try {
            if(sqliteDB.databaseFileExists()) {
                // Try reusing old install, fail quietly
                try {
                    FormplayerConfigEngine engine = new FormplayerConfigEngine(storageFactory, formplayerInstallerFactory, formplayerArchiveFileRoot);
                    engine.initEnvironment();
                    return engine;
                } catch (Exception e) {
                    log.error("Got exception " + e + " while reinitializing at path " + sqliteDB.getDatabaseFileForDebugPurposes());
                }
            }
            // Wipe out folder and attempt install
            sqliteDB.closeConnection();
            sqliteDB.deleteDatabaseFile();
            if (!sqliteDB.databaseFolderExists() && !sqliteDB.createDatabaseFolder()) {
                throw new RuntimeException("Error instantiationing folder " + sqliteDB.getDatabaseFileForDebugPurposes());
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
            log.error("Got exception " + e + " while installing reference " + reference + " at path " + sqliteDB.getDatabaseFileForDebugPurposes());
            throw new UnresolvedResourceRuntimeException(e);
        } catch (Exception e) {
            log.error("Got exception " + e + " while installing reference " + reference + " at path " + sqliteDB.getDatabaseFileForDebugPurposes());
            sqliteDB.deleteDatabaseFile();
            throw e;
        }
    }
}
