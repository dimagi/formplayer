package services;

import install.FormplayerConfigEngine;

import java.io.IOException;

/**
 * Created by willpride on 2/25/16.
 */
public interface InstallService {
    public FormplayerConfigEngine configureApplication(String reference, String username) throws IOException;
}
