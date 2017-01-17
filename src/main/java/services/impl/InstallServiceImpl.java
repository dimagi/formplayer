package services.impl;

import engine.FormplayerConfigEngine;
import installers.FormplayerInstallerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.resources.model.InstallCancelledException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.engine.CommCareConfigEngine;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import services.FormplayerStorageFactory;
import services.InstallService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.net.URL;

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

    private final Log log = LogFactory.getLog(InstallServiceImpl.class);

    @Override
    public CommCareConfigEngine configureApplication(String reference) {
        String dbPath = storageFactory.getDatabasePath();
        log.info("Configuring application with reference " + reference + " and dbPath: " + dbPath + ".");
        try {
            File dbFolder = new File(dbPath);
            if(dbFolder.exists()) {
                // Try reusing old install, fail quietly
                try {
                    CommCareConfigEngine engine = new FormplayerConfigEngine(storageFactory, formplayerInstallerFactory);
                    engine.initEnvironment();
                    return engine;
                } catch (Exception e) {
                    log.error("Got exception " + e + " while reinitializing at path " + dbPath);
                }
            }
            // Wipe out folder and attempt install
            SqlSandboxUtils.deleteDatabaseFolder(dbPath);
            if (!dbFolder.mkdirs()) {
                throw new RuntimeException("Error instantiationing folder " + dbFolder);
            }
            CommCareConfigEngine engine = new FormplayerConfigEngine(storageFactory, formplayerInstallerFactory);
            if (reference.endsWith(".ccz")) {
                engine.initFromArchive(reference);
            } else {
                engine.initFromLocalFileResource(reference);
            }
            engine.initEnvironment();
            return engine;
        } catch (InstallCancelledException | UnresolvedResourceException | UnfullfilledRequirementsException e) {
            log.error("Got exception " + e + " while installing reference " + reference + " at path " + dbPath);
            SqlSandboxUtils.deleteDatabaseFolder(dbPath);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("Got exception " + e + " while installing reference " + reference + " at path " + dbPath);
            SqlSandboxUtils.deleteDatabaseFolder(dbPath);
            throw e;
        }
    }
}
