package services.impl;

import install.FormplayerConfigEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.resources.model.InstallCancelledException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import services.InstallService;

import java.io.IOException;

/**
 * Created by willpride on 2/25/16.
 */
public class InstallServiceImpl implements InstallService {

    private final Log log = LogFactory.getLog(InstallServiceImpl.class);

    private final String host;

    public InstallServiceImpl(String host){
        this.host = host;
    }

    @Override
    public FormplayerConfigEngine configureApplication(String reference, String username, String dbPath) throws IOException, InstallCancelledException, UnresolvedResourceException, UnfullfilledRequirementsException {
        log.info("Configuring application with reference " + reference + " and dbPath: " + dbPath + ".");
        FormplayerConfigEngine engine = new FormplayerConfigEngine(username, dbPath);
        reference = host + reference;
        engine.initFromArchive(reference);
        engine.initEnvironment();
        return engine;
    }
}
