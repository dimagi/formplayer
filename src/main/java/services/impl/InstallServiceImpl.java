package services.impl;

import install.FormplayerConfigEngine;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.commcare.resources.model.InstallCancelledException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import services.InstallService;

import java.io.File;
import java.io.IOException;

/**
 * Created by willpride on 2/25/16.
 */
public class InstallServiceImpl implements InstallService {
    @Override
    public FormplayerConfigEngine configureApplication(String reference, String username, String dbPath) throws IOException, InstallCancelledException, UnresolvedResourceException, UnfullfilledRequirementsException {
        System.out.println("User: " + username + " dbpath: " + dbPath);
        FormplayerConfigEngine engine = new FormplayerConfigEngine(username, dbPath);
        if(reference.endsWith(".ccz")){
            engine.initFromArchive(reference);
        } else if(reference.endsWith(".ccpr")) {
            engine.initFromLocalFileResource(reference);
        } else {
            throw new RuntimeException("Can't instantiate with reference: " + reference);
        }
        engine.initEnvironment();
        return engine;
    }
}
