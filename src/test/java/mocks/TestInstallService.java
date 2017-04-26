package mocks;

import engine.FormplayerConfigEngine;
import services.impl.InstallServiceImpl;

import java.io.File;
import java.net.URL;

/**
 * Created by willpride on 12/7/16.
 */
public class TestInstallService extends InstallServiceImpl {

    @Override
    public FormplayerConfigEngine configureApplication(String reference) throws Exception {
        return super.configureApplication(getTestResourcePath(reference));
    }

    private String getTestResourcePath(String resourcePath){
        try {
            URL url = this.getClass().getClassLoader().getResource(resourcePath);
            File file = new File(url.getPath());
            return file.getAbsolutePath();
        } catch(NullPointerException npe){
            npe.printStackTrace();
            throw npe;
        }
    }
}
