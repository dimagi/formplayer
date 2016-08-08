package util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.StackFrameStep;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.util.NoLocalizedTextException;

import java.util.Arrays;
import java.util.Vector;

/**
 * Created by willpride on 4/14/16.
 */
public class SessionUtils {

    private static final Log log = LogFactory.getLog(SessionUtils.class);

    public static String getBestTitle(SessionWrapper session){

        String[] stepTitles;
        try {
            stepTitles = session.getHeaderTitles();
        } catch (NoLocalizedTextException e) {
            // localization resources may not be installed while in the middle
            // of an update, so default to a generic title
            return null;
        }

        Vector<StackFrameStep> v = session.getFrame().getSteps();

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

    public static void setLocale(String locale){
        if(locale == null || "".equals(locale.trim())){
            return;
        }
        Localizer localizer = Localization.getGlobalLocalizerAdvanced();
        log.info("Setting locale to : " + locale + " available: " + localizer.getAvailableLocales());
        for (String availabile : localizer.getAvailableLocales()) {
            if (locale.equals(availabile)) {
                localizer.setLocale(locale);

                return;
            }
        }
    }
}
