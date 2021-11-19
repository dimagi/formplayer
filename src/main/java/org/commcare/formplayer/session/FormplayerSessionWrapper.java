package org.commcare.formplayer.session;

import lombok.SneakyThrows;
import org.commcare.core.interfaces.RemoteInstanceFetcher;
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
    private RemoteInstanceFetcher remoteInstanceFetcher;

    public FormplayerSessionWrapper(CommCarePlatform platform, UserSandbox sandbox,
                                    RemoteInstanceFetcher remoteInstanceFetcher) throws RemoteInstanceFetcher.RemoteInstanceException {
        this(platform, sandbox, new SessionFrame(), remoteInstanceFetcher);
    }

    public FormplayerSessionWrapper(CommCarePlatform platform, UserSandbox sandbox, SessionFrame sessionFrame,
                                    RemoteInstanceFetcher remoteInstanceFetcher) throws RemoteInstanceFetcher.RemoteInstanceException {
        super(platform, sandbox);
        this.frame = sessionFrame;
        this.remoteInstanceFetcher = remoteInstanceFetcher;
        prepareExternalSources(remoteInstanceFetcher);
    }

    public FormplayerSessionWrapper(CommCareSession session, CommCarePlatform platform, UserSandbox sandbox,
                                    RemoteInstanceFetcher remoteInstanceFetcher) throws RemoteInstanceFetcher.RemoteInstanceException {
        super(session, platform, sandbox);
        this.remoteInstanceFetcher = remoteInstanceFetcher;
        prepareExternalSources(remoteInstanceFetcher);
    }


    @Override
    public CommCareInstanceInitializer getIIF() {
        if (initializer == null) {
            initializer = new FormplayerInstanceInitializer(this, (UserSqlSandbox) mSandbox, mPlatform);
        }
        return initializer;
    }
}
