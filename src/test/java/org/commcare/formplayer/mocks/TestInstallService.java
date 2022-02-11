package org.commcare.formplayer.mocks;

import org.commcare.formplayer.engine.FormplayerConfigEngine;
import org.commcare.formplayer.services.InstallService;
import org.commcare.modern.util.Pair;

import java.io.File;
import java.net.URL;

/**
 * Created by willpride on 12/7/16.
 */
public class TestInstallService extends InstallService {

    @Override
    public Pair<FormplayerConfigEngine, Boolean> configureApplication(String reference,
            boolean preview) throws Exception {
        return super.configureApplication(getTestResourcePath(reference), preview);
    }

    private String getTestResourcePath(String resourcePath) {
        try {
            URL url = this.getClass().getClassLoader().getResource(resourcePath);
            File file = new File(url.getPath());
            return file.getAbsolutePath();
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            throw npe;
        }
    }
}
