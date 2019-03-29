package services;

import engine.FormplayerConfigEngine;
import exceptions.UnresolvedResourceRuntimeException;
import installers.FormplayerInstallerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.commcare.modern.util.Pair;
import org.commcare.resources.model.UnresolvedResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sqlitedb.SQLiteDB;
import util.Constants;
import util.SimpleTimer;

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

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    private final Log log = LogFactory.getLog(InstallService.class);

    CategoryTimingHelper.RecordingTimer installTimer;

    public Pair<FormplayerConfigEngine, Boolean> configureApplication(String reference, boolean preview) throws Exception {
        boolean newInstall = true;
        SQLiteDB sqliteDB = storageFactory.getSQLiteDB();
        log.info("Configuring application with reference " + reference +
                " and dbPath: " + sqliteDB.getDatabaseFileForDebugPurposes() + " \n" +
                "and storage factory \" + storageFactory");
        try {
            if (sqliteDB.databaseFileExists()) {
                newInstall = false;
                // If the SQLiteDB exists then this was not an update
                // Try reusing old install, fail quietly
                try {
                    FormplayerConfigEngine engine = new FormplayerConfigEngine(storageFactory, formplayerInstallerFactory, formplayerArchiveFileRoot);
                    engine.initEnvironment();
                    return new Pair<>(engine, false);
                } catch (Exception e) {
                    log.warn("Warning: Failed to use the existing sandbox located at path " + sqliteDB.getDatabaseFileForDebugPurposes() +
                            " because of the exception " + e);
                }
            }

            // Wipe out folder and attempt install
            sqliteDB.closeConnection();
            sqliteDB.deleteDatabaseFile();
            installTimer = categoryTimingHelper.newTimer(Constants.TimingCategories.APP_INSTALL);
            installTimer.start();
            if (!sqliteDB.databaseFolderExists() && !sqliteDB.createDatabaseFolder()) {
                throw new RuntimeException("Error instantiating folder " + sqliteDB.getDatabaseFileForDebugPurposes());
            }
            FormplayerConfigEngine engine = new FormplayerConfigEngine(storageFactory, formplayerInstallerFactory, formplayerArchiveFileRoot);
            if (reference.endsWith(".ccpr")) {
                engine.initFromLocalFileResource(reference);
            } else {
                engine.initFromArchive(reference, preview);
            }
            engine.initEnvironment();
            installTimer.end();
            installTimer.record();
            return new Pair<>(engine, newInstall);
        } catch (UnresolvedResourceException e) {
            log.error("Got exception " + e + " while installing reference " + reference + " at path " + sqliteDB.getDatabaseFileForDebugPurposes());
            throw new UnresolvedResourceRuntimeException(e);
        } catch (Exception e) {
            log.error("Got exception " + e + " while installing reference " + reference + " at path " + sqliteDB.getDatabaseFileForDebugPurposes());
            sqliteDB.deleteDatabaseFile();
            throw e;
        }
    }

    public SimpleTimer getInstallTimer() {
        return installTimer;
    }
}
