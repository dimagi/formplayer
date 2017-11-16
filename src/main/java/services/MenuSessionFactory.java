package services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.util.screen.MenuScreen;
import org.commcare.util.screen.Screen;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import repo.FormSessionRepo;
import repo.SerializableMenuSession;
import session.MenuSession;

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

    public MenuSession rebuildSessionFromFrame(SessionFrame frame,
                                               String username,
                                               String domain,
                                               String appId,
                                               String installReference,
                                               String locale,
                                               boolean oneQuestionPerScreen,
                                               String asUser,
                                               boolean preview) throws Exception {

        MenuSession menuSession = buildSession(username,
                domain, appId, installReference, locale,
                oneQuestionPerScreen, asUser, preview);

        Screen screen = menuSession.getNextScreen();
        Vector<StackFrameStep> steps = frame.getSteps();
        for (StackFrameStep step: steps) {
            String currentStep = null;
            switch (step.getElementType()) {
                case SessionFrame.STATE_COMMAND_ID:
                    String stepId = step.getId();
                    MenuScreen menuScreen = (MenuScreen) screen;
                    for (int i = 0; i < menuScreen.getMenuDisplayables().length; i++) {
                        MenuDisplayable menuDisplayable = menuScreen.getMenuDisplayables()[i];
                        if (menuDisplayable.getCommandID().equals(stepId)) {
                            currentStep = String.valueOf(i);
                        }
                    }
                    break;
                case SessionFrame.STATE_DATUM_VAL:
                    currentStep = step.getValue();
                    break;
                default:
                    log.info(String.format("Could not get command for step %s on screen %s", step, screen));
                    break;
            }
            if (currentStep != null) {
                menuSession.addSelection(currentStep);
                menuSession.handleInput(currentStep);
            }
        }
        return menuSession;
    }

    public MenuSession buildSession(String username,
                                    String domain,
                                    String appId,
                                    String installReference,
                                    String locale,
                                    boolean oneQuestionPerScreen,
                                    String asUser,
                                    boolean preview) throws Exception {
        return new MenuSession(username, domain, appId, installReference, locale,
                installService, restoreFactory, host, oneQuestionPerScreen, asUser, preview);
    }

    public MenuSession buildSession(SerializableMenuSession serializableMenuSession) throws Exception {
        return new MenuSession(serializableMenuSession, installService, restoreFactory, host);
    }
}
