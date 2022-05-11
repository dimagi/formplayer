package org.commcare.formplayer.session;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.model.Case;
import org.commcare.core.interfaces.VirtualDataInstanceCache;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.formplayer.database.models.FormplayerCaseIndexTable;
import org.commcare.formplayer.engine.FormplayerIndexedFixtureInstanceTreeElement;
import org.commcare.formplayer.sandbox.SqlStorage;
import org.commcare.formplayer.sandbox.UserSqlSandbox;
import org.commcare.session.SessionFrame;
import org.commcare.session.SessionInstanceBuilder;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.User;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ConcreteInstanceRoot;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.InstanceRoot;
import org.javarosa.core.model.instance.TreeElement;

import java.util.Hashtable;

/**
 * Created by willpride on 1/29/16.
 */
public class FormplayerInstanceInitializer extends CommCareInstanceInitializer {


    public FormplayerInstanceInitializer(UserSqlSandbox sandbox) {
        super(sandbox);
    }

    public FormplayerInstanceInitializer(FormplayerSessionWrapper formplayerSessionWrapper,
                                         UserSqlSandbox mSandbox, CommCarePlatform mPlatform) {
        super(formplayerSessionWrapper, mSandbox, mPlatform);
    }

    @Override
    protected InstanceRoot setupCaseData(ExternalDataInstance instance) {
        if (casebase == null) {
            SqlStorage<Case> storage = (SqlStorage<Case>)mSandbox.getCaseStorage();
            FormplayerCaseIndexTable formplayerCaseIndexTable;
            formplayerCaseIndexTable = new FormplayerCaseIndexTable((UserSqlSandbox)mSandbox);
            casebase = new CaseInstanceTreeElement(instance.getBase(), storage, formplayerCaseIndexTable);
        } else {
            //re-use the existing model if it exists.
            casebase.rebase(instance.getBase());
        }
        //instance.setCacheHost((AndroidCaseInstanceTreeElement)casebase);
        return new ConcreteInstanceRoot(casebase);
    }

    @Override
    protected InstanceRoot setupSessionData(ExternalDataInstance instance) {
        if (this.mPlatform == null) {
            throw new RuntimeException("Cannot generate session instance with undeclared platform!");
        }
        User u = mSandbox.getLoggedInUser();
        if (u == null) {
            throw new RuntimeException("There was a problem loading the user data. Please Sync.");
        }

        Hashtable<String, String> userProperties = u.getProperties();

        TreeElement root =
                SessionInstanceBuilder.getSessionInstance(session.getFrame(), getDeviceId(),
                        getVersionString(), getCurrentDrift(), u.getUsername(), u.getUniqueId(),
                        userProperties);
        root.setParent(instance.getBase());
        return new ConcreteInstanceRoot(root);
    }

    @Override
    protected InstanceRoot setupFixtureData(ExternalDataInstance instance) {
        AbstractTreeElement indexedFixture = FormplayerIndexedFixtureInstanceTreeElement.get(
                mSandbox,
                getRefId(instance.getReference()),
                instance.getBase());

        if (indexedFixture != null) {
            return new ConcreteInstanceRoot(indexedFixture);
        } else {
            return new ConcreteInstanceRoot(loadFixtureRoot(instance, instance.getReference()));
        }
    }

    public String getVersionString() {
        return "Formplayer Version: " + mPlatform.getMajorVersion() + "." + mPlatform.getMinorVersion();
    }

    @Override
    protected String getDeviceId() {
        return "Formplayer";
    }
}
