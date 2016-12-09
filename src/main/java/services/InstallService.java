package services;

import org.commcare.util.engine.CommCareConfigEngine;
import org.springframework.stereotype.Service;

/**
 * Created by willpride on 2/25/16.
 */
@Service
public interface InstallService {
    CommCareConfigEngine configureApplication(String reference);
}
