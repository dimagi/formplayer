package session;

import engine.FormplayerCaseInstanceTreeElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.cases.model.Case;
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
public class FormplayerInstanceInitializer extends CommCareInstanceInitializer {

    private final Map<String, String> injectedSessionData;
    private final Log log = LogFactory.getLog(FormplayerInstanceInitializer.class);

    public FormplayerInstanceInitializer(FormplayerSessionWrapper formplayerSessionWrapper,
                                         UserSandbox mSandbox, CommCarePlatform mPlatform,
                                         Map<String, String> injectedSessionData) {
        super(formplayerSessionWrapper, mSandbox, mPlatform);
        this.injectedSessionData = injectedSessionData;
    }

    @Override
    protected AbstractTreeElement setupCaseData(ExternalDataInstance instance) {
        if (casebase == null) {
            SqliteIndexedStorageUtility<Case> storage = (SqliteIndexedStorageUtility<Case>) mSandbox.getCaseStorage();
            casebase = new FormplayerCaseInstanceTreeElement(instance.getBase(), storage);
        } else {
            //re-use the existing model if it exists.
            casebase.rebase(instance.getBase());
        }
        //instance.setCacheHost((AndroidCaseInstanceTreeElement)casebase);
        return casebase;
    }

    @Override
    protected AbstractTreeElement setupSessionData(ExternalDataInstance instance) {
        if (this.mPlatform == null) {
            throw new RuntimeException("Cannot generate session instance with undeclared platform!");
        }
        User u = mSandbox.getLoggedInUser();
        if (u == null) {
            throw new RuntimeException("There was a problem loading the user data. Please Sync.");
        }
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

    public String getVersionString(){
        return "Formplayer Version: " + mPlatform.getMajorVersion() + "." + mPlatform.getMinorVersion();
    }

    @Override
    protected String getDeviceId() {
        return "Formplayer";
    }
}
