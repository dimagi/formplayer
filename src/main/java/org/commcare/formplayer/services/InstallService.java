package org.commcare.formplayer.services;

import org.commcare.formplayer.engine.FormplayerConfigEngine;
import org.commcare.formplayer.exceptions.UnresolvedResourceRuntimeException;
import org.commcare.formplayer.installers.FormplayerInstallerFactory;

import datadog.trace.api.Trace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.commcare.modern.util.Pair;
import org.commcare.resources.model.UnresolvedResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import org.commcare.formplayer.sqlitedb.SQLiteDB;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.SimpleTimer;
import org.springframework.web.client.RestTemplate;

/**
 * The InstallService handles configuring the application,
 * either from a .ccz or .ccpr reference or existing dbs.
 * This can involve app download, install, and initialization of resources.
 */
@Service
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class InstallService {

    @Autowired
    FormplayerStorageFactory storageFactory;

    @Autowired
    FormplayerInstallerFactory formplayerInstallerFactory;

    @Autowired
    ArchiveFileRoot formplayerArchiveFileRoot;

    @Autowired
    private CategoryTimingHelper categoryTimingHelper;

    @Autowired
    private RestTemplate restTemplate;

    private final Log log = LogFactory.getLog(InstallService.class);

    CategoryTimingHelper.RecordingTimer installTimer;

    @Trace
    public Pair<FormplayerConfigEngine, Boolean> configureApplication(String reference, boolean preview) throws Exception {
        SQLiteDB sqliteDB = storageFactory.getSQLiteDB();
        log.info("Configuring application with reference " + reference +
                " and dbPath: " + sqliteDB.getDatabaseFileForDebugPurposes() + " \n" +
                "and storage factory \" + storageFactory");
        try {
            if (sqliteDB.databaseFileExists()) {
                // If the SQLiteDB exists then this was not an update
                // Try reusing old install, fail quietly
                try {
                    FormplayerConfigEngine engine = new FormplayerConfigEngine(
                            storageFactory, formplayerInstallerFactory, formplayerArchiveFileRoot, restTemplate
                    );
                    engine.initEnvironment();
                    return new Pair<>(engine, false);
                } catch (Exception e) {
                    log.debug("An error occurred while trying to use the old DB file for app. Error details: Got exception "
                            + e + " while reinitializing at path " + sqliteDB.getDatabaseFileForDebugPurposes() + " Reinitializing new DB ..");
                }
            }

            // Wipe out folder and attempt install
            sqliteDB.closeConnection();
            sqliteDB.deleteDatabaseFile();
            installTimer = categoryTimingHelper.newTimer(Constants.TimingCategories.APP_INSTALL, storageFactory.getDomain());
            installTimer.start();
            if (!sqliteDB.databaseFolderExists() && !sqliteDB.createDatabaseFolder()) {
                throw new RuntimeException("Error instantiating folder " + sqliteDB.getDatabaseFileForDebugPurposes());
            }
            FormplayerConfigEngine engine = new FormplayerConfigEngine(
                    storageFactory, formplayerInstallerFactory, formplayerArchiveFileRoot, restTemplate
            );
            if (reference.endsWith(".ccpr")) {
                engine.initFromLocalFileResource(reference);
            } else {
                engine.initFromArchive(reference, preview);
            }
            engine.initEnvironment();
            installTimer.end();
            installTimer.record();
            return new Pair<>(engine, true);
        } catch (UnresolvedResourceException e) {
            throw new UnresolvedResourceRuntimeException(e);
        } catch (Exception e) {
            sqliteDB.deleteDatabaseFile();
            throw e;
        }
    }

    public SimpleTimer getInstallTimer() {
        return installTimer;
    }
}
