package services.impl;

import engine.FormplayerConfigEngine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.util.engine.CommCareConfigEngine;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import services.InstallService;

import java.io.File;

/**
 * Created by willpride on 2/25/16.
 */
public class InstallServiceImpl implements InstallService {

    private final Log log = LogFactory.getLog(InstallServiceImpl.class);

    @Override
    public CommCareConfigEngine configureApplication(String reference, final String username, final String dbPath) {
        log.info("Configuring application with reference " + reference + " and dbPath: " + dbPath + ".");
        try {
            final String trimmedUsername = StringUtils.substringBefore(username, "@");

            File dbFolder = new File(dbPath);
            SqlSandboxUtils.deleteDatabaseFolder(dbPath);
            dbFolder.mkdirs();

            CommCareConfigEngine.setStorageFactory(new IStorageIndexedFactory() {
                @Override
                public IStorageUtilityIndexed newStorage(String name, Class type) {
                    return new SqliteIndexedStorageUtility(type, name, trimmedUsername, dbPath);
                }
            });

            CommCareConfigEngine engine = new FormplayerConfigEngine(PrototypeManager.getDefault());
            engine.initFromArchive(reference);
            engine.initEnvironment();
            return engine;
        } catch (Exception e) {
            log.error("Got exception " + e + " while installing reference " + reference + " at path " + dbPath);
            SqlSandboxUtils.deleteDatabaseFolder(dbPath);
            throw new RuntimeException(e);
        }
    }
}
