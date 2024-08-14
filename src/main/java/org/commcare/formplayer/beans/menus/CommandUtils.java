package org.commcare.formplayer.beans.menus;

import org.commcare.session.CommCareSession;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.SessionDatum;

public class CommandUtils {

    public enum NavIconState {
        NEXT, JUMP, ENTITY_SELECT
    }

    public static NavIconState getIconState(MenuDisplayable menuDisplayable, CommCareSession session) {
        NavIconState iconChoice = NavIconState.NEXT;

        // Figure out some icons
        if (menuDisplayable instanceof Entry) {
            SessionDatum datum = session.getNeededDatum((Entry)menuDisplayable);
            if (datum == null || !(datum instanceof EntityDatum)) {
                iconChoice = NavIconState.JUMP;
            }
        }
        return iconChoice;
    }
}
