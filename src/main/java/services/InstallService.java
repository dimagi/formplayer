package services;

import install.FormplayerConfigEngine;
import org.commcare.resources.model.InstallCancelledException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;

import java.io.IOException;

/**
 * Created by willpride on 2/25/16.
 */
public interface InstallService {
    public FormplayerConfigEngine configureApplication(String reference, String username, String dbPath) throws IOException, InstallCancelledException, UnresolvedResourceException, UnfullfilledRequirementsException;
}
