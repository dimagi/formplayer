package services;

import org.commcare.util.engine.CommCareConfigEngine;

/**
 * Created by willpride on 2/25/16.
 */
public interface InstallService {
    CommCareConfigEngine configureApplication(String reference, String username, String dbPath);
}
