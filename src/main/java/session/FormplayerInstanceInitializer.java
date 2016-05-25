package session;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.session.SessionInstanceBuilder;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.User;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;

import java.util.Map;

/**
 * Created by willpride on 1/29/16.
 */
class FormplayerInstanceInitializer extends CommCareInstanceInitializer {

    private final Map<String, String> injectedSessionData;

    public FormplayerInstanceInitializer(FormplayerSessionWrapper formplayerSessionWrapper,
                                         UserSandbox mSandbox, CommCarePlatform mPlatform,
                                         Map<String, String> injectedSessionData) {
        super(formplayerSessionWrapper, mSandbox, mPlatform);
        this.injectedSessionData = injectedSessionData;
    }

    @Override
    protected AbstractTreeElement setupSessionData(ExternalDataInstance instance) {
        if (this.mPlatform == null) {
            throw new RuntimeException("Cannot generate session instance with undeclared platform!");
        }
        User u = mSandbox.getLoggedInUser();
        if(injectedSessionData != null) {
            for (String key : injectedSessionData.keySet()) {
                session.setDatum(key, injectedSessionData.get(key));
            }
        }
        TreeElement root =
                SessionInstanceBuilder.getSessionInstance(session.getFrame(), getDeviceId(),
                        getVersionString(), u.getUsername(), u.getUniqueId(),
                        u.getProperties()).getRoot();
        root.setParent(instance.getBase());
        return root;
    }
}
