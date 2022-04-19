package org.commcare.formplayer.session;

import org.commcare.core.interfaces.EntitiesSelectionCache;
import org.commcare.core.interfaces.RemoteInstanceFetcher;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionFrame;
import org.commcare.util.CommCarePlatform;

/**
 * Created by willpride on 1/29/16.
 */
class FormplayerSessionWrapper extends SessionWrapper {
    private RemoteInstanceFetcher remoteInstanceFetcher;
    private EntitiesSelectionCache entitiesSelectionCache;

    public FormplayerSessionWrapper(CommCarePlatform platform, UserSandbox sandbox,
            RemoteInstanceFetcher remoteInstanceFetcher, EntitiesSelectionCache entitiesSelectionCache)
            throws RemoteInstanceFetcher.RemoteInstanceException {
        this(platform, sandbox, new SessionFrame(), remoteInstanceFetcher, entitiesSelectionCache);
    }

    public FormplayerSessionWrapper(CommCarePlatform platform, UserSandbox sandbox, SessionFrame sessionFrame,
            RemoteInstanceFetcher remoteInstanceFetcher, EntitiesSelectionCache entitiesSelectionCache)
            throws RemoteInstanceFetcher.RemoteInstanceException {
        super(platform, sandbox);
        this.frame = sessionFrame;
        this.remoteInstanceFetcher = remoteInstanceFetcher;
        this.entitiesSelectionCache = entitiesSelectionCache;
        prepareExternalSources(remoteInstanceFetcher);
    }

    public FormplayerSessionWrapper(CommCareSession session, CommCarePlatform platform, UserSandbox sandbox,
            RemoteInstanceFetcher remoteInstanceFetcher, EntitiesSelectionCache entitiesSelectionCache)
            throws RemoteInstanceFetcher.RemoteInstanceException {
        super(session, platform, sandbox);
        this.remoteInstanceFetcher = remoteInstanceFetcher;
        this.entitiesSelectionCache = entitiesSelectionCache;
        prepareExternalSources(remoteInstanceFetcher);
    }


    @Override
    public CommCareInstanceInitializer getIIF() {
        if (initializer == null) {
            initializer = new FormplayerInstanceInitializer(this, (UserSqlSandbox)mSandbox, mPlatform,
                    entitiesSelectionCache);
        }
        return initializer;
    }
}
