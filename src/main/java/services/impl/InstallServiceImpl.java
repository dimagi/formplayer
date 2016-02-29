package services.impl;

import install.FormplayerConfigEngine;
import org.apache.commons.io.output.ByteArrayOutputStream;
import services.InstallService;

import java.io.IOException;

/**
 * Created by willpride on 2/25/16.
 */
public class InstallServiceImpl implements InstallService {
    @Override
    public FormplayerConfigEngine configureApplication(String reference, String username) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FormplayerConfigEngine engine = new FormplayerConfigEngine(baos, username);
        if(reference.endsWith(".ccz")){
            engine.initFromArchive(reference);
        } else{
            throw new RuntimeException("Can't instantiate with reference: " + reference);
        }
        engine.initEnvironment();
        return engine;
    }
}
