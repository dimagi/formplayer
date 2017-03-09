package session;

import database.models.FormplayerCaseIndexTable;
import engine.FormplayerCaseInstanceTreeElement;
import sandbox.SqliteIndexedStorageUtility;
import sandbox.UserSqlSandbox;
import org.commcare.cases.model.Case;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.session.SessionInstanceBuilder;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.User;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;

import java.util.Hashtable;
import java.util.Map;

/**
 * Created by willpride on 1/29/16.
 */
public class FormplayerInstanceInitializer extends CommCareInstanceInitializer {

    private Map<String, String> sessionData;

    public FormplayerInstanceInitializer(UserSqlSandbox sandbox) {
        super(sandbox);
    }

    public FormplayerInstanceInitializer(FormplayerSessionWrapper formplayerSessionWrapper,
                                         UserSqlSandbox mSandbox, CommCarePlatform mPlatform,
                                         Map<String, String> sessionData) {
        super(formplayerSessionWrapper, mSandbox, mPlatform);
        this.sessionData = sessionData;
    }

    @Override
    protected AbstractTreeElement setupCaseData(ExternalDataInstance instance) {
        if (casebase == null) {
            SqliteIndexedStorageUtility<Case> storage = (SqliteIndexedStorageUtility<Case>) mSandbox.getCaseStorage();
            FormplayerCaseIndexTable formplayerCaseIndexTable;
            formplayerCaseIndexTable = new FormplayerCaseIndexTable((UserSqlSandbox) mSandbox);
            casebase = new FormplayerCaseInstanceTreeElement(instance.getBase(), storage, formplayerCaseIndexTable);
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
        if (sessionData != null) {
            for (String key : sessionData.keySet()) {
                session.setDatum(key, sessionData.get(key));
            }
        }

        Hashtable<String, String> userProperties = u.getProperties();

        TreeElement root =
                SessionInstanceBuilder.getSessionInstance(session.getFrame(), getDeviceId(),
                        getVersionString(), u.getUsername(), u.getUniqueId(),
                        userProperties).getRoot();
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
