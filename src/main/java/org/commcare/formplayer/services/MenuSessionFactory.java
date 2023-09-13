package org.commcare.formplayer.services;

import com.google.common.collect.ImmutableMultimap;
import datadog.trace.api.Trace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.core.interfaces.RemoteInstanceFetcher;
import org.commcare.formplayer.engine.FormplayerConfigEngine;
import org.commcare.formplayer.objects.SerializableMenuSession;
import org.commcare.formplayer.session.MenuSession;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.*;
import org.commcare.util.screen.*;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

/**
 * Class containing logic for accepting a NewSessionRequest and services,
 * restoring the user, opening the new form, and returning the question list response.
 */
@Component
public class MenuSessionFactory {

    private static final String NEXT_SCREEN = "NEXT_SCREEN";

    @Autowired
    private RestoreFactory restoreFactory;

    @Autowired
    private InstallService installService;

    @Autowired
    private CaseSearchHelper caseSearchHelper;

    @Autowired
    protected FormplayerStorageFactory storageFactory;

    @Autowired
    private VirtualDataInstanceService virtualDataInstanceService;

    @Value("${commcarehq.host}")
    private String host;

    private static final Log log = LogFactory.getLog(MenuSessionFactory.class);

    /**
     * Rebuild the MenuSession from its stack frame. This is used after end of form navigation.
     * By re-walking the frame, we establish the set of selections the user 'would' have made to get
     * to this state without doing end of form navigation. Such a path must always exist in a valid app.
     */
    @Trace
    public void rebuildSessionFromFrame(MenuSession menuSession, CaseSearchHelper caseSearchHelper) throws CommCareSessionException, RemoteInstanceFetcher.RemoteInstanceException {
        List<StackFrameStep> steps = menuSession.getSessionWrapper().getFrame().getSteps().stream().filter(
                step -> !step.getType().equals(SessionFrame.STATE_MARK)
        ).toList();
        menuSession.resetSession();
        EntityScreenContext entityScreenContext = new EntityScreenContext();
        Screen screen = menuSession.getNextScreen(false, entityScreenContext);
        int processedStepsCount = 0;
        boolean needsFullInit = false;
        while (screen != null && processedStepsCount < steps.size()) {
            String input = null;
            StackFrameStep currentStep = steps.get(processedStepsCount);
            if (screen instanceof MenuScreen) {
                if (menuSession.autoAdvanceMenu(screen, storageFactory.getPropertyManager().isAutoAdvanceMenu())) {
                    screen = menuSession.getNextScreen(needsFullInit, entityScreenContext);
                    continue;
                }

                MenuDisplayable[] options = ((MenuScreen)screen).getMenuDisplayables();
                for (int i = 0; i < options.length; i++) {
                    if (currentStep.getId().equals(options[i].getCommandID())) {
                        input = String.valueOf(i);
                        // final step, needs to init fully to show to screen
                        needsFullInit = ++processedStepsCount == steps.size();
                    }
                }
            } else if (screen instanceof EntityScreen) {
                EntityScreen entityScreen = (EntityScreen)screen;
                entityScreen.initReferences(menuSession.getSessionWrapper());
                SessionDatum neededDatum = entityScreen.getSession().getNeededDatum();
                Vector<Action> actions = entityScreen.getShortDetail().getCustomActions(entityScreen.getEvalContext());
                outer:
                if (currentStep.getType().equals(SessionFrame.STATE_DATUM_VAL) && currentStep.getId().equals(neededDatum.getDataId())) {
                    if (entityScreen.referencesContainStep(currentStep.getValue())) {
                        input = currentStep.getValue();
                        needsFullInit = ++processedStepsCount == steps.size();
                    }
                } else if (currentStep.getType().equals(SessionFrame.STATE_COMMAND_ID) && !actions.isEmpty()) {
                    // this is messy and gross, but we need to find the action that corresponds to this step
                    for (int i = 0; i < actions.size(); i++) {
                        Action action = actions.get(i);
                        if (action.getStackOperations() != null) {
                            for (StackOperation operation : action.getStackOperations()) {
                                // TODO: does this need to 'consume' session steps for all the steps in the operation?
                                Optional<StackFrameStep> commandStep = operation.getStackFrameSteps().stream()
                                        .filter(s -> s.getType().equals(SessionFrame.STATE_COMMAND_ID)).findFirst();
                                if (commandStep.isEmpty()) {
                                    continue;
                                }
                                String value = commandStep.get().evaluateValue(entityScreen.getEvalContext());
                                if (value.equals(currentStep.getId())) {
                                    input = "action " + i;
                                    needsFullInit = ++processedStepsCount == steps.size();
                                    break outer;
                                }
                            }
                        }
                    }
                }

                // Only init subscreen if we are not going to auto-launch a different screen
                String nextInput = input == null ? "" : input;
                entityScreen.evaluateAutoLaunch(nextInput);
                if (entityScreen.getAutoLaunchAction() == null) {
                    entityScreen.initListSubScreen();
                }

                if (input != null && input != NEXT_SCREEN && entityScreen.shouldBeSkipped()) {
                    menuSession.handleInput(input, needsFullInit, true, false, entityScreenContext);
                    screen = menuSession.getNextScreen(needsFullInit, entityScreenContext);
                    continue;
                }
            } else if (screen instanceof QueryScreen) {
                QueryScreen queryScreen = (QueryScreen)screen;
                RemoteQueryDatum neededDatum = (RemoteQueryDatum) menuSession.getSessionWrapper().getNeededDatum();
                if (currentStep.getId().equals(neededDatum.getDataId())) {
                    URI uri = null;
                    try {
                        uri = new URI(currentStep.getValue());
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        throw new CommCareSessionException("Query URL format error: " + e.getMessage(), e);
                    }
                    ImmutableMultimap.Builder<String, String> dataBuilder = ImmutableMultimap.builder();
                    currentStep.getExtras().forEach((key, value) -> dataBuilder.put(key, (String) value));
                        try {
                            ExternalDataInstance searchDataInstance = caseSearchHelper.getRemoteDataInstance(
                                queryScreen.getQueryDatum().getDataId(),
                                queryScreen.getQueryDatum().useCaseTemplate(),
                                uri.toURL(),
                                dataBuilder.build(),
                                false
                            );
                            queryScreen.updateSession(searchDataInstance, dataBuilder.build());
                            needsFullInit = ++processedStepsCount == steps.size();
                            screen = menuSession.getNextScreen(needsFullInit, entityScreenContext);
                            input = NEXT_SCREEN;
                    } catch (InvalidStructureException | IOException | XmlPullParserException | UnfullfilledRequirementsException e) {
                        e.printStackTrace();
                        throw new CommCareSessionException("Query response format error: " + e.getMessage(), e);
                    }
                }
            }
            if (input == null) {
                break;
            } else if (input != NEXT_SCREEN) {
                menuSession.handleInput(input, needsFullInit, true, false, entityScreenContext);
                menuSession.addSelection(input);
                screen = menuSession.getNextScreen(needsFullInit, entityScreenContext);
            }
        }
    }

    @Trace
    public MenuSession buildSession(String username,
                                    String domain,
                                    String appId,
                                    String locale,
                                    boolean oneQuestionPerScreen,
                                    String asUser,
                                    boolean preview) throws Exception {
        return new MenuSession(username, domain, appId, locale,
                installService, restoreFactory, host, oneQuestionPerScreen, asUser, preview,
                new FormplayerRemoteInstanceFetcher(caseSearchHelper, virtualDataInstanceService));
    }

    @Trace
    public MenuSession buildSession(SerializableMenuSession serializableMenuSession, FormplayerConfigEngine engine,
            CommCareSession commCareSession) throws Exception {
        return new MenuSession(serializableMenuSession, engine, commCareSession, restoreFactory,
                new FormplayerRemoteInstanceFetcher(caseSearchHelper, virtualDataInstanceService));
    }
}
