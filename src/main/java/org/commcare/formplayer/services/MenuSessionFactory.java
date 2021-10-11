package org.commcare.formplayer.services;

import org.commcare.suite.model.RemoteQueryDatum;
import java.net.URI;
import java.net.URISyntaxException;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.util.screen.CommCareSessionException;
import org.commcare.util.screen.EntityScreen;
import org.commcare.util.screen.MenuScreen;
import org.commcare.util.screen.QueryScreen;
import org.commcare.util.screen.Screen;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.commcare.formplayer.objects.SerializableMenuSession;
import org.commcare.formplayer.session.MenuSession;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
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

    @Autowired
    protected FormplayerStorageFactory storageFactory;

    @Value("${commcarehq.host}")
    private String host;

    private static final Log log = LogFactory.getLog(MenuSessionFactory.class);

    public void rebuildSessionFromFrame(MenuSession menuSession) throws CommCareSessionException {
        rebuildSessionFromFrame(menuSession, null);
    }

    /**
     * Rebuild the MenuSession from its stack frame. This is used after end of form navigation.
     * By re-walking the frame, we establish the set of selections the user 'would' have made to get
     * to this state without doing end of form navigation. Such a path must always exist in a valid app.
     */
    public void rebuildSessionFromFrame(MenuSession menuSession, CaseSearchHelper caseSearchHelper) throws CommCareSessionException {
        Vector<StackFrameStep> steps = menuSession.getSessionWrapper().getFrame().getSteps();
        menuSession.resetSession();
        Screen screen = menuSession.getNextScreen(false);
        while (screen != null) {
            String currentStep = null;
            if (screen instanceof MenuScreen) {
                MenuDisplayable[] options = ((MenuScreen)screen).getMenuDisplayables();
                for (int i = 0; i < options.length; i++) {
                    for (StackFrameStep step : steps) {
                        if (step.getId().equals(options[i].getCommandID())) {
                            currentStep = String.valueOf(i);
                        }
                    }
                }
            } else if (screen instanceof EntityScreen) {
                EntityScreen entityScreen = (EntityScreen)screen;
                entityScreen.init(menuSession.getSessionWrapper());
                if (entityScreen.shouldBeSkipped()) {
                    screen = menuSession.getNextScreen(false);
                    continue;
                }
                SessionDatum neededDatum = entityScreen.getSession().getNeededDatum();
                for (StackFrameStep step : steps) {
                    if (step.getId().equals(neededDatum.getDataId())) {
                        if (entityScreen.referencesContainStep(step.getValue())) {
                            currentStep = step.getValue();
                        }
                        break;
                    }
                }
            } else if (screen instanceof QueryScreen) {
                QueryScreen queryScreen = (QueryScreen)screen;
                RemoteQueryDatum neededDatum = (RemoteQueryDatum) menuSession.getSessionWrapper().getNeededDatum();
                boolean done = false;
                for (StackFrameStep step : steps) {
                    if (step.getId().equals(neededDatum.getDataId())) {
                        URI uri = null;
                        try {
                            uri = new URI(step.getValue());
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                            throw new CommCareSessionException("Query URL format error: " + e.getMessage());
                        }
                        try {
                            ExternalDataInstance searchDataInstance = caseSearchHelper.getRemoteDataInstance(
                                queryScreen.getQueryDatum().getDataId(),
                                queryScreen.getQueryDatum().useCaseTemplate(),
                                uri
                            );
                            queryScreen.setQueryDatum(searchDataInstance);
                            screen = menuSession.getNextScreen(false);
                            done = true;
                        } catch (InvalidStructureException | IOException | XmlPullParserException | UnfullfilledRequirementsException e) {
                            e.printStackTrace();
                            throw new CommCareSessionException("Query response format error: " + e.getMessage());
                        }
                    }
                }
                if (done) {
                    continue;
                }
            }
            if (currentStep == null) {
                break;
            } else {
                menuSession.handleInput(currentStep, false, true, false, storageFactory.getPropertyManager().isAutoAdvanceMenu());
                menuSession.addSelection(currentStep);
                screen = menuSession.getNextScreen(false);
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
