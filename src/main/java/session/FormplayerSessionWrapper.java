package session;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.util.CommCarePlatform;

import java.util.Map;

/**
 * Created by willpride on 1/29/16.
 */
class FormplayerSessionWrapper extends SessionWrapper {

    private final Map<String, String> injectedSessionData;

    public FormplayerSessionWrapper(CommCarePlatform platform, UserSandbox sandbox) {
        this(platform, sandbox, null);
    }

    public FormplayerSessionWrapper(CommCarePlatform platform, UserSandbox sandbox, Map<String, String> injectedSessionData) {
        super(platform, sandbox);
        this.injectedSessionData = injectedSessionData;
    }

    public FormplayerSessionWrapper(CommCareSession session, CommCarePlatform platform, UserSandbox sandbox) {
        super(session, platform, sandbox);
        this.injectedSessionData = null;
    }

    @Override
    public CommCareInstanceInitializer getIIF() {
        if (initializer == null) {
            initializer = new FormplayerInstanceInitializer(this, mSandbox, mPlatform, injectedSessionData);
        }
        return initializer;
    }
}
