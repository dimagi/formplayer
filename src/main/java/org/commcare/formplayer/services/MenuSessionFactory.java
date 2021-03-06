package org.commcare.formplayer.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.util.screen.CommCareSessionException;
import org.commcare.util.screen.EntityScreen;
import org.commcare.util.screen.MenuScreen;
import org.commcare.util.screen.Screen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.commcare.formplayer.repo.SerializableMenuSession;
import org.commcare.formplayer.session.MenuSession;

import java.util.Set;
import java.util.Vector;

/**
 * Class containing logic for accepting a NewSessionRequest and services,
 * restoring the user, opening the new form, and returning the question list response.
 */
@Component
public class MenuSessionFactory {

    @Autowired
    private RestoreFactory restoreFactory;

    @Autowired
    private InstallService installService;

    @Value("${commcarehq.host}")
    private String host;

    private static final Log log = LogFactory.getLog(MenuSessionFactory.class);

    /**
     * Rebuild the MenuSession from its stack frame. This is used after end of form navigation.
     * By re-walking the frame, we establish the set of selections the user 'would' have made to get
     * to this state without doing end of form navigation. Such a path must always exist in a valid app.
    */
    public void rebuildSessionFromFrame(MenuSession menuSession) throws CommCareSessionException {
        Vector<StackFrameStep> steps = menuSession.getSessionWrapper().getFrame().getSteps();
        menuSession.resetSession();
        Screen screen = menuSession.getNextScreen();
        while (screen != null) {
            String currentStep = null;
            if (screen instanceof MenuScreen) {
                MenuDisplayable[] options = ((MenuScreen) screen).getMenuDisplayables();
                for (int i = 0; i < options.length; i++) {
                    for (StackFrameStep step : steps) {
                        if (step.getId().equals(options[i].getCommandID())) {
                            currentStep = String.valueOf(i);
                        }
                    }
                }
            } else if (screen instanceof EntityScreen) {
                EntityScreen entityScreen = (EntityScreen) screen;
                entityScreen.init(menuSession.getSessionWrapper());
                if (entityScreen.shouldBeSkipped()) {
                    screen = menuSession.getNextScreen();
                    continue;
                }
                SessionDatum neededDatum = entityScreen.getSession().getNeededDatum();
                Set<String> entityIds = entityScreen.getReferenceMap().keySet();
                for (StackFrameStep step: steps) {
                    if (step.getId().equals(neededDatum.getDataId())) {
                        if (entityIds.contains(step.getValue())) {
                            currentStep = step.getValue();
                        }
                    }
                }
            }
            if (currentStep == null) {
                break;
            } else {
                menuSession.handleInput(currentStep);
                menuSession.addSelection(currentStep);
                screen = menuSession.getNextScreen();
            }
        }
    }

    public MenuSession buildSession(String username,
                                    String domain,
                                    String appId,
                                    String locale,
                                    boolean oneQuestionPerScreen,
                                    String asUser,
                                    boolean preview) throws Exception {
        return new MenuSession(username, domain, appId, locale,
                installService, restoreFactory, host, oneQuestionPerScreen, asUser, preview);
    }

    public MenuSession buildSession(SerializableMenuSession serializableMenuSession) throws Exception {
        return new MenuSession(serializableMenuSession, installService, restoreFactory, host);
    }
}
