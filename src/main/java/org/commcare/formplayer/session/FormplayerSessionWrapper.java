package org.commcare.formplayer.session;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionFrame;
import org.commcare.util.CommCarePlatform;
import org.commcare.formplayer.sandbox.UserSqlSandbox;

import java.util.Map;

/**
 * Created by willpride on 1/29/16.
 */
class FormplayerSessionWrapper extends SessionWrapper {

    private final Map<String, String> sessionData;

    public FormplayerSessionWrapper(CommCarePlatform platform, UserSandbox sandbox) {
        this(platform, sandbox, null, new SessionFrame());
    }

    public FormplayerSessionWrapper(CommCarePlatform platform, UserSandbox sandbox,
                                    Map<String, String> sessionData, SessionFrame sessionFrame) {
        super(platform, sandbox);
        this.sessionData = sessionData;
        this.frame = sessionFrame;
    }

    public FormplayerSessionWrapper(CommCareSession session, CommCarePlatform platform, UserSandbox sandbox) {
        super(session, platform, sandbox);
        this.sessionData = null;
    }

    @Override
    public CommCareInstanceInitializer getIIF() {
        if (initializer == null) {
            initializer = new FormplayerInstanceInitializer(this, (UserSqlSandbox) mSandbox, mPlatform);
        }
        return initializer;
    }
}
