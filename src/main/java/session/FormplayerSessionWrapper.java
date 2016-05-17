package session;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.util.NoLocalizedTextException;

import java.util.Map;
import java.util.Vector;

/**
 * Created by willpride on 1/29/16.
 */
public class FormplayerSessionWrapper extends SessionWrapper {

    private Map<String, String> injectedSessionData;

    public FormplayerSessionWrapper(CommCarePlatform platform, UserSandbox sandbox, Map<String, String> injectedSessionData) {
        super(platform, sandbox);
        this.injectedSessionData = injectedSessionData;
    }

    @Override
    public CommCareInstanceInitializer getIIF() {
        if (initializer == null) {
            initializer = new FormplayerInstanceInitializer(this, mSandbox, mPlatform, injectedSessionData);
        }

        return initializer;
    }
}
