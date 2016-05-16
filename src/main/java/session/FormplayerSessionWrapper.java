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

    private String getBestTitleHelper() {
        
        String[] stepTitles;
        try {
            stepTitles = getHeaderTitles();
        } catch (NoLocalizedTextException e) {
            // localization resources may not be installed while in the middle
            // of an update, so default to a generic title
            return null;
        }

        Vector<StackFrameStep> v = getFrame().getSteps();

        //So we need to work our way backwards through each "step" we've taken, since our RelativeLayout
        //displays the Z-Order b insertion (so items added later are always "on top" of items added earlier
        String bestTitle = null;
        for (int i = v.size() - 1; i >= 0; i--) {
            if (bestTitle != null) {
                break;
            }
            StackFrameStep step = v.elementAt(i);

            if (!SessionFrame.STATE_DATUM_VAL.equals(step.getType())) {
                bestTitle = stepTitles[i];
            }
        }
        return bestTitle;
    }
}
