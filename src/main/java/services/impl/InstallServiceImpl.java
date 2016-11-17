package services.impl;

import install.FormplayerConfigEngine;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.resources.model.InstallCancelledException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.engine.CommCareConfigEngine;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import services.InstallService;

import java.io.File;
import java.io.IOException;

/**
 * Created by willpride on 2/25/16.
 */
public class InstallServiceImpl implements InstallService {

    private final Log log = LogFactory.getLog(InstallServiceImpl.class);

    @Override
    public CommCareConfigEngine configureApplication(String reference, final String username, final String dbPath) throws IOException, InstallCancelledException, UnresolvedResourceException, UnfullfilledRequirementsException {
        log.info("Configuring application with reference " + reference + " and dbPath: " + dbPath + ".");

        final String trimmedUsername = StringUtils.substringBefore(username, "@");

        File dbFolder = new File(dbPath);
        dbFolder.delete();
        dbFolder.mkdirs();

        CommCareConfigEngine.setStorageFactory(new IStorageIndexedFactory() {
            @Override
            public IStorageUtilityIndexed newStorage(String name, Class type) {
                return new SqliteIndexedStorageUtility(type, name, trimmedUsername, dbPath);
            }
        });

        CommCareConfigEngine engine = new CommCareConfigEngine(PrototypeManager.getDefault());
        engine.initFromArchive(reference);
        engine.initEnvironment();
        return engine;
    }
}
