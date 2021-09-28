package org.commcare.formplayer.services;

import io.sentry.Sentry;

import okhttp3.HttpUrl;

import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.beans.menus.BaseResponseBean;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityDetailListResponse;
import org.commcare.formplayer.beans.menus.EntityDetailResponse;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.MenuBean;
import org.commcare.formplayer.beans.menus.QueryResponseBean;
import org.commcare.formplayer.beans.auth.FeatureFlagChecker;
import org.commcare.formplayer.beans.menus.*;
import org.commcare.formplayer.exceptions.ApplicationConfigException;
import org.commcare.formplayer.objects.FormVolatilityRecord;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.screens.FormplayerQueryScreen;
import org.commcare.formplayer.screens.FormplayerSyncScreen;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.session.MenuSession;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerDatadog;
import org.commcare.formplayer.util.FormplayerHereFunctionHandler;
import org.commcare.formplayer.util.SessionUtils;

import datadog.trace.api.Trace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.commcare.formplayer.util.Constants.TOGGLE_SESSION_ENDPOINTS;
import org.commcare.formplayer.web.client.WebClient;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Endpoint;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.suite.model.StackOperation;
import org.commcare.suite.model.Text;
import org.commcare.util.screen.CommCareSessionException;
import org.commcare.util.screen.EntityScreen;
import org.commcare.util.screen.MenuScreen;
import org.commcare.util.screen.QueryScreen;
import org.commcare.util.screen.Screen;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.util.OrderedHashtable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.annotation.Resource;

import io.sentry.Sentry;

/**
 * Class containing logic for accepting a NewSessionRequest and services,
 * restoring the user, opening the new form, and returning the question list response.
 */
@Component
public class MenuSessionRunnerService {

    @Autowired
    private RestoreFactory restoreFactory;

    @Autowired
    private InstallService installService;

    @Autowired
    private CaseSearchHelper caseSearchHelper;

    @Autowired
    private WebClient webClient;

    @Autowired
    protected FormSessionService formSessionService;

    @Autowired
    protected MenuSessionService menuSessionService;

    @Autowired
    protected MenuSessionFactory menuSessionFactory;

    @Autowired
    protected FormSendCalloutHandler formSendCalloutHandler;

    @Autowired
    protected FormplayerStorageFactory storageFactory;

    @Autowired
    private RedisTemplate redisVolatilityDict;

    @Autowired
    private FormplayerDatadog datadog;

    @Autowired
    protected NewFormResponseFactory newFormResponseFactory;

    @Resource(name = "redisVolatilityDict")
    private ValueOperations<String, FormVolatilityRecord> volatilityCache;

    private static final Log log = LogFactory.getLog(MenuSessionRunnerService.class);

    public BaseResponseBean getNextMenu(MenuSession menuSession) throws Exception {
        return getNextMenu(menuSession, null, 0, "", 0, null, 0, null);
    }

    @Trace
    private BaseResponseBean getNextMenu(MenuSession menuSession,
                                         String detailSelection,
                                         int offset,
                                         String searchText,
                                         int sortIndex,
                                         QueryData queryData,
                                         int casesPerPage,
                                         String smartLinkTemplate) throws Exception {
        Screen nextScreen = menuSession.getNextScreen();
        BaseResponseBean smartResponse = getSmartResponse(menuSession, smartLinkTemplate);
        if (smartResponse != null) {
            return smartResponse;
        }

        // No next menu screen? Start form entry!
        if (nextScreen == null) {
            String assertionFailure = getAssertionFailure(menuSession);
            if (assertionFailure != null) {
                BaseResponseBean responseBean = new BaseResponseBean("App Configuration Error",
                        new NotificationMessage(assertionFailure, NotificationMessage.Type.app_error, NotificationMessage.Tag.menu),
                        true);
                return responseBean;
            }
            return startFormEntry(menuSession);
        }

        MenuBean menuResponseBean;
        // We're looking at a module or form menu
        if (nextScreen instanceof MenuScreen) {
            menuResponseBean = new CommandListResponseBean(
                    (MenuScreen)nextScreen,
                    menuSession.getSessionWrapper(),
                    menuSession.getId()
            );
            datadog.addRequestScopedTag(Constants.MODULE_TAG, "menu");
            Sentry.setTag(Constants.MODULE_TAG, "menu");
        } else if (nextScreen instanceof EntityScreen) {
            // We're looking at a case list or detail screen
            nextScreen.init(menuSession.getSessionWrapper());
            if (nextScreen.shouldBeSkipped()) {
                return getNextMenu(menuSession, detailSelection, offset, searchText, sortIndex, queryData, casesPerPage, smartLinkTemplate);
            }
            addHereFuncHandler((EntityScreen)nextScreen, menuSession);
            menuResponseBean = new EntityListResponse(
                    (EntityScreen)nextScreen,
                    detailSelection,
                    offset,
                    searchText,
                    sortIndex,
                    storageFactory.getPropertyManager().isFuzzySearchEnabled(),
                    casesPerPage
            );
            datadog.addRequestScopedTag(Constants.MODULE_TAG, "case_list");
            Sentry.setTag(Constants.MODULE_TAG, "case_list");
            // using getBestTitle to eliminate risk of showing private information
            String caseListName = SessionUtils.getBestTitle(menuSession.getSessionWrapper());
            datadog.addRequestScopedTag(Constants.MODULE_NAME_TAG, caseListName);
            Sentry.setTag(Constants.MODULE_NAME_TAG, caseListName);
        } else if (nextScreen instanceof FormplayerQueryScreen) {
            ((FormplayerQueryScreen)nextScreen).refreshItemSetChoices();
            String queryKey = menuSession.getSessionWrapper().getCommand();
            if (queryData != null && !queryData.getExecute(queryKey)) {
                answerQueryPrompts((FormplayerQueryScreen)nextScreen, queryData.getInputs(queryKey));
            }
            menuResponseBean = new QueryResponseBean(
                    (QueryScreen)nextScreen,
                    menuSession.getSessionWrapper()
            );
            datadog.addRequestScopedTag(Constants.MODULE_TAG, "case_search");
            Sentry.setTag(Constants.MODULE_TAG, "case_search");
        } else {
            throw new Exception("Unable to recognize next screen: " + nextScreen);
        }

        menuResponseBean.setBreadcrumbs(menuSession.getBreadcrumbs());
        menuResponseBean.setAppId(menuSession.getAppId());
        menuResponseBean.setAppVersion(menuSession.getCommCareVersionString() + ", App Version: " + menuSession.getAppVersion());
        menuResponseBean.setPersistentCaseTile(getPersistentDetail(menuSession, storageFactory.getPropertyManager().isFuzzySearchEnabled()));
        return menuResponseBean;
    }

    private BaseResponseBean getSmartResponse(MenuSession menuSession, String template) {
        if (template == null || template.equals("")) {
            return null;
        }

        SessionWrapper session = menuSession.getSessionWrapper();
        String command = session.getCommand();
        if (command != null) {
            Endpoint endpoint = menuSession.getEndpointByCommand(command);
            if (endpoint != null) {
                template = template.replace("---", endpoint.getId());
                HttpUrl.Builder urlBuilder = HttpUrl.parse(template).newBuilder();
                OrderedHashtable<String, String> data = session.getData();
                for (String key : data.keySet()) {
                    if (endpoint.getArguments().contains(key)) {
                        urlBuilder.addQueryParameter(key, data.get(key));
                    }
                }
                String finalUrl = urlBuilder.build().toString();
                BaseResponseBean responseBean = new BaseResponseBean(null, null, true);
                System.out.println("final url => " + finalUrl);
                responseBean.setSmartLinkRedirect(finalUrl);
                return responseBean;
            }
        }

        return null;
    }

    private void addHereFuncHandler(EntityScreen nextScreen, MenuSession menuSession) {
        EvaluationContext ec = nextScreen.getEvalContext();
        ec.addFunctionHandler(new FormplayerHereFunctionHandler(menuSession, menuSession.getCurrentBrowserLocation()));
    }

    @Trace
    public BaseResponseBean advanceSessionWithSelections(MenuSession menuSession,
                                                         String[] selections) throws Exception {
        return advanceSessionWithSelections(menuSession, selections, null, null,
                0, null, 0, false, 0, null);
    }

    /**
     * Advances the session based on the selections.
     *
     * @param selections      - Selections are either an integer index into a list of modules
     *                        or a case id indicating the case selected for a case detail.
     *                        <p>
     *                        An example selection would be ["0", "2", "6c5d91e9-61a2-4264-97f3-5d68636ff316"]
     *                        <p>
     *                        This would mean select the 0th menu, then the 2nd menu, then the case with the id 6c5d91e9-61a2-4264-97f3-5d68636ff316.
     * @param detailSelection - If requesting a case detail will be a case id, else null. When the case id is given
     *                        it is used to short circuit the normal TreeReference calculation by inserting a predicate that
     *                        is [@case_id = <detailSelection>].
     */
    @Trace
    public BaseResponseBean advanceSessionWithSelections(MenuSession menuSession,
                                                         String[] selections,
                                                         String detailSelection,
                                                         QueryData queryData,
                                                         int offset,
                                                         String searchText,
                                                         int sortIndex,
                                                         boolean forceManualAction,
                                                         int casesPerPage,
                                                         String smartLinkTemplate) throws Exception {
        BaseResponseBean nextResponse;
        boolean needsDetail;
        // If we have no selections, we're are the root screen.
        if (selections == null) {
            return getNextMenu(
                    menuSession,
                    null,
                    offset,
                    searchText,
                    sortIndex,
                    queryData,
                    casesPerPage,
                    smartLinkTemplate
            );
        }
        NotificationMessage notificationMessage = null;
        boolean rebuildSession = false;
        for (int i = 1; i <= selections.length; i++) {
            String selection = selections[i - 1];

            boolean confirmed = restoreFactory.isConfirmedSelection(Arrays.copyOfRange(selections, 0, i));

            // minimal entity screens are only safe if there will be no further selection
            // and we do not need the case detail
            needsDetail = detailSelection != null || i != selections.length;
            boolean gotNextScreen = menuSession.handleInput(selection, needsDetail, confirmed, true, isAutoAdvanceMenu());
            if (!gotNextScreen) {
                notificationMessage = new NotificationMessage(
                        "Overflowed selections with selection " + selection + " at index " + i,
                        true,
                        NotificationMessage.Tag.selection);
                break;
            }
            Screen nextScreen = menuSession.getNextScreen(needsDetail);

            String nextInput = i == selections.length ? "" : selections[i];
            // Advance the session in case auto launch is set
            nextScreen = handleAutoLaunch(nextScreen, menuSession, selection, needsDetail, confirmed, nextInput);
            boolean replay = i < selections.length;
            notificationMessage = handleQueryScreen(nextScreen, menuSession, queryData, replay, forceManualAction);
            if (nextScreen instanceof FormplayerSyncScreen) {
                BaseResponseBean syncResponse = doSyncGetNext(
                        (FormplayerSyncScreen)nextScreen,
                        menuSession);
                if (syncResponse != null) {
                    return syncResponse;
                }
            }

            if (nextScreen == null && menuSession.getSessionWrapper().getForm() == null) {
                // we don't have a resolution, try rebuilding session to execute any pending ops
                executeAndRebuildSession(menuSession);
                rebuildSession = true;
            } else {
                menuSession.addSelection(selection);
            }
        }

        nextResponse = getNextMenu(
                menuSession,
                detailSelection,
                offset,
                searchText,
                sortIndex,
                queryData,
                casesPerPage,
                smartLinkTemplate
        );
        restoreFactory.cacheSessionSelections(selections);

        if (nextResponse != null) {
            if (nextResponse.getNotification() == null && notificationMessage != null) {
                nextResponse.setNotification(notificationMessage);
            }
            log.info("Returning menu: " + nextResponse);
            nextResponse.setSelections(menuSession.getSelections());
            return nextResponse;
        } else {
            BaseResponseBean responseBean = new BaseResponseBean(null,
                    new NotificationMessage(null, false, NotificationMessage.Tag.selection),
                    true);
            return responseBean;
        }
    }

    private NotificationMessage handleQueryScreen(Screen nextScreen, MenuSession menuSession, QueryData queryData,
                                                  boolean replay, boolean forceManualAction)
            throws CommCareSessionException {
        if (nextScreen instanceof FormplayerQueryScreen) {
            FormplayerQueryScreen formplayerQueryScreen = ((FormplayerQueryScreen)nextScreen);
            formplayerQueryScreen.refreshItemSetChoices();
            boolean autoSearch = replay || (formplayerQueryScreen.doDefaultSearch() && !forceManualAction);
            String queryKey = menuSession.getSessionWrapper().getCommand();
            if ((queryData != null && queryData.getExecute(queryKey)) || autoSearch) {
                return doQuery(
                        (FormplayerQueryScreen)nextScreen,
                        menuSession,
                        queryData == null ? null : queryData.getInputs(queryKey),
                        formplayerQueryScreen.doDefaultSearch() && !forceManualAction
                );
            } else if (queryData != null) {
                answerQueryPrompts((FormplayerQueryScreen)nextScreen,
                        queryData.getInputs(queryKey));
            }
        }
        return null;
    }

    private boolean isAutoAdvanceMenu() {
        return storageFactory.getPropertyManager().isAutoAdvanceMenu();
    }

    private Screen handleAutoLaunch(Screen nextScreen, MenuSession menuSession,
                                    String selection, boolean needsDetail, boolean confirmed, String nextInput)
            throws CommCareSessionException {
        if (nextScreen instanceof EntityScreen) {
            EntityScreen entityScreen = (EntityScreen)nextScreen;
            entityScreen.evaluateAutoLaunch(nextInput);
            if (entityScreen.getAutoLaunchAction() != null) {
                menuSession.handleInput(selection, needsDetail, confirmed, true, isAutoAdvanceMenu());
                nextScreen = menuSession.getNextScreen(needsDetail);
            }
        }
        return nextScreen;
    }

    // Sets the query fields and refreshes any itemset choices based on them
    private void answerQueryPrompts(FormplayerQueryScreen screen,
                                    Hashtable<String, String> queryDictionary) {
        if (queryDictionary != null) {
            screen.answerPrompts(queryDictionary);
        }
        screen.refreshItemSetChoices();
    }


    /**
     * Perform the sync and update the notification and screen accordingly.
     * After a sync, we can either pop another menu/form to begin
     * or just return to the app menu.
     */
    @Trace
    private BaseResponseBean doSyncGetNext(FormplayerSyncScreen nextScreen,
                                           MenuSession menuSession) throws Exception {
        NotificationMessage notificationMessage = doSync(nextScreen);

        BaseResponseBean postSyncResponse = resolveFormGetNext(menuSession);
        if (postSyncResponse != null) {
            // If not null, we have a form or menu to redirect to
            if (notificationMessage != null) {
                postSyncResponse.setNotification(notificationMessage);
            }
            return postSyncResponse;
        } else {
            // Otherwise, return use to the app root
            postSyncResponse = new BaseResponseBean(null,
                    new NotificationMessage("Redirecting after sync", false, NotificationMessage.Tag.sync),
                    true);
            return postSyncResponse;
        }
    }

    private NotificationMessage doSync(FormplayerSyncScreen screen) throws Exception {
        try {
            webClient.post(screen.getUrl(), screen.getQueryParams());
        } catch (RestClientResponseException e) {
            return new NotificationMessage(
                    String.format("Case claim failed. Message: %s", e.getResponseBodyAsString()), true, NotificationMessage.Tag.sync);
        } catch (RestClientException e) {
            return new NotificationMessage("Unknown error performing case claim", true, NotificationMessage.Tag.sync);
        }
        restoreFactory.performTimedSync(false, false, false);
        return null;
    }

    /**
     * If we've encountered a QueryScreen and have a QueryDictionary, do the query
     * and update the session, screen, and notification message accordingly.
     * <p>
     * Will do nothing if this wasn't a query screen.
     */
    @Trace
    private NotificationMessage doQuery(FormplayerQueryScreen screen,
                                        MenuSession menuSession,
                                        Hashtable<String, String> queryDictionary,
                                        boolean skipDefaultPromptValues) throws CommCareSessionException {
        log.info("Formplayer doing query with dictionary " + queryDictionary);
        NotificationMessage notificationMessage = null;

        if (queryDictionary != null) {
            screen.answerPrompts(queryDictionary);
        }

        String error = null;
        ExternalDataInstance searchDataInstance;
        try {
            searchDataInstance = searchAndSetResult(
                    screen,
                    screen.getUri(skipDefaultPromptValues));
            if (searchDataInstance == null) {
                error = "No result from query";
            }
        } catch (InvalidStructureException | IOException
                | XmlPullParserException | UnfullfilledRequirementsException e) {
            e.printStackTrace();
            error = "Query response format error: " + e.getMessage();
        }

        if (error != null) {
            notificationMessage = new NotificationMessage(
                    "Query failed with message " + error,
                    true,
                    NotificationMessage.Tag.query);
        } else {
            notificationMessage = new NotificationMessage(screen.getCurrentMessage(), false, NotificationMessage.Tag.query);
        }

        Screen nextScreen = menuSession.getNextScreen();
        log.info("Next screen after query: " + nextScreen);
        return notificationMessage;
    }

    public ExternalDataInstance searchAndSetResult(FormplayerQueryScreen screen, URI uri)
            throws UnfullfilledRequirementsException, XmlPullParserException, IOException, InvalidStructureException {
        ExternalDataInstance searchDataInstance = caseSearchHelper.getSearchDataInstance(screen.getQueryDatum().getDataId(),
                screen.getQueryDatum().useCaseTemplate(), uri);
        screen.setQueryDatum(searchDataInstance);
        return searchDataInstance;
    }

    @Trace
    public BaseResponseBean resolveFormGetNext(MenuSession menuSession) throws Exception {
        if (executeAndRebuildSession(menuSession)) {
            Screen nextScreen = menuSession.getNextScreen();
            nextScreen = handleAutoLaunch(nextScreen, menuSession, "", false, false, "");
            handleQueryScreen(nextScreen, menuSession, new QueryData(), false, false);
            BaseResponseBean response = getNextMenu(menuSession);
            response.setSelections(menuSession.getSelections());
            return response;
        }
        return null;
    }

    // Rebuild the session after executing any pending session stack
    private boolean executeAndRebuildSession(MenuSession menuSession) throws CommCareSessionException {
        menuSession.getSessionWrapper().syncState();
        if (menuSession.getSessionWrapper().finishExecuteAndPop(menuSession.getSessionWrapper().getEvaluationContext())) {
            menuSession.getSessionWrapper().clearVolatiles();
            menuSessionFactory.rebuildSessionFromFrame(menuSession);
            return true;
        }
        return false;
    }

    protected static TreeReference getReference(SessionWrapper session, EntityDatum entityDatum) {
        EvaluationContext ec = session.getEvaluationContext();
        StackFrameStep stepToFrame = getStepToFrame(session);
        String caseId = stepToFrame.getValue();
        TreeReference reference = entityDatum.getEntityFromID(ec, caseId);
        if (reference == null) {
            throw new ApplicationConfigException(String.format("Could not create tile for case with ID %s " +
                    "because this case does not meet the criteria for the case list with ID %s.", caseId, entityDatum.getShortDetail()));
        }
        return reference;
    }

    public static EntityDetailListResponse getInlineDetail(MenuSession menuSession, boolean isFuzzySearchEnabled) {
        return getDetail(menuSession, true, isFuzzySearchEnabled);
    }

    public static EntityDetailResponse getPersistentDetail(MenuSession menuSession, boolean isFuzzySearchEnabled) {
        EntityDetailListResponse detailListResponse = getDetail(menuSession, false, isFuzzySearchEnabled);
        if (detailListResponse == null) {
            return null;
        }
        EntityDetailResponse[] detailList = detailListResponse.getEntityDetailList();
        if (detailList == null) {
            return null;
        }
        return detailList[0];
    }

    private static EntityDetailListResponse getDetail(MenuSession menuSession, boolean inline, boolean isFuzzySearchEnabled) {
        SessionWrapper session = menuSession.getSessionWrapper();
        StackFrameStep stepToFrame = getStepToFrame(session);
        if (stepToFrame == null) {
            return null;
        }
        EntityDatum entityDatum = session.findDatumDefinition(stepToFrame.getId());
        if (entityDatum == null) {
            return null;
        }
        String detailId;
        if (inline) {
            detailId = entityDatum.getInlineDetail();
        } else {
            detailId = entityDatum.getPersistentDetail();
        }
        if (detailId == null) {
            return null;
        }
        Detail persistentDetail = session.getDetail(detailId);
        TreeReference reference = getReference(session, entityDatum);

        EvaluationContext ec;
        if (inline) {
            ec = menuSession.getEvalContextWithHereFuncHandler();
            return new EntityDetailListResponse(persistentDetail.getFlattenedDetails(), ec, reference, isFuzzySearchEnabled);
        } else {
            ec = new EvaluationContext(menuSession.getEvalContextWithHereFuncHandler(), reference);
            EntityDetailResponse detailResponse = new EntityDetailResponse(persistentDetail, ec);
            detailResponse.setHasInlineTile(entityDatum.getInlineDetail() != null);
            return new EntityDetailListResponse(detailResponse);
        }
    }

    private static StackFrameStep getStepToFrame(SessionWrapper session) {
        StackFrameStep stepToFrame = null;
        Vector<StackFrameStep> v = session.getFrame().getSteps();
        //So we need to work our way backwards through each "step" we've taken, since our RelativeLayout
        //displays the Z-Order b insertion (so items added later are always "on top" of items added earlier
        for (int i = v.size() - 1; i >= 0; i--) {
            StackFrameStep step = v.elementAt(i);

            if (SessionFrame.STATE_DATUM_VAL.equals(step.getType())) {
                //Only add steps which have a tile.
                EntityDatum entityDatum = session.findDatumDefinition(step.getId());
                if (entityDatum != null && entityDatum.getPersistentDetail() != null) {
                    stepToFrame = step;
                }
            }
        }
        return stepToFrame;
    }

    @Trace
    private NewFormResponse startFormEntry(MenuSession menuSession) throws Exception {
        if (menuSession.getSessionWrapper().getForm() != null) {
            NewFormResponse formResponseBean = generateFormEntrySession(menuSession);
            formResponseBean.setAppId(menuSession.getAppId());
            formResponseBean.setAppVersion(menuSession.getCommCareVersionString() + ", App Version: " + menuSession.getAppVersion());
            formResponseBean.setPersistentCaseTile(getPersistentDetail(menuSession, storageFactory.getPropertyManager().isFuzzySearchEnabled()));
            formResponseBean.setBreadcrumbs(menuSession.getBreadcrumbs());
            // update datadog/sentry metrics
            datadog.addRequestScopedTag(Constants.MODULE_TAG, "form");
            Sentry.setTag(Constants.MODULE_TAG, "form");
            String formName = formResponseBean.getTitle();
            datadog.addRequestScopedTag(Constants.FORM_NAME_TAG, formName);
            Sentry.setTag(Constants.FORM_NAME_TAG, formName);
            return formResponseBean;
        } else {
            return null;
        }
    }

    private String getAssertionFailure(MenuSession menuSession) {
        Text text = menuSession.getSessionWrapper().getCurrentEntry().getAssertions().getAssertionFailure(menuSession.getEvalContextWithHereFuncHandler());
        if (text != null) {
            return text.evaluate(menuSession.getEvalContextWithHereFuncHandler());
        }
        return null;
    }


    @Trace
    private NewFormResponse generateFormEntrySession(MenuSession menuSession) throws Exception {
        menuSessionService.saveSession(menuSession.serialize());
        FormSession formEntrySession = menuSession.getFormEntrySession(formSendCalloutHandler, storageFactory, caseSearchHelper);

        NewFormResponse response = newFormResponseFactory.getResponse(formEntrySession);
        response.setNotification(establishVolatility(formEntrySession));
        response.setShouldAutoSubmit(formEntrySession.getAutoSubmitFlag());
        return response;
    }

    private NotificationMessage establishVolatility(FormSession session) {
        FormVolatilityRecord newRecord = session.getSessionVolatilityRecord();
        if (volatilityCache != null && newRecord != null) {
            FormVolatilityRecord existingRecord = volatilityCache.get(newRecord.getKey());

            //Overwrite any existing records unless they were from a submissions, since submission
            //records are more relevant
            if (existingRecord == null || !existingRecord.wasSubmitted()) {
                newRecord.updateFormOpened(session);
                newRecord.write(volatilityCache);
            }

            if (existingRecord != null && !existingRecord.matchesUser(session)) {
                return existingRecord.getNotificationIfRelevant(restoreFactory.getLastSyncTime());
            }
        }
        return null;
    }

    public BaseResponseBean advanceSessionWithEndpoint(MenuSession menuSession, String endpointId, @Nullable HashMap<String, String> endpointArgs)
            throws Exception {
        if (!FeatureFlagChecker.isToggleEnabled(TOGGLE_SESSION_ENDPOINTS)) {
            throw new RuntimeException("Linking into applications has been disabled for this project.");
        }

        Endpoint endpoint = menuSession.getEndpoint(endpointId);
        if (endpoint == null) {
            throw new RuntimeException("This link does not exist. Your app may have changed so that the given link is no longer valid");
        }
        SessionWrapper sessionWrapper = menuSession.getSessionWrapper();
        EvaluationContext evalContext = sessionWrapper.getEvaluationContext();
        try {
            if (endpointArgs != null) {
                Endpoint.populateEndpointArgumentsToEvaluationContext(endpoint, endpointArgs, evalContext);
            }
        } catch (Endpoint.InvalidEndpointArgumentsException ieae) {
            String missingMessage = "";
            if (ieae.hasMissingArguments()) {
                missingMessage = String.format(" Missing arguments: %s.", String.join(", ", ieae.getMissingArguments()));
            }
            String unexpectedMessage = "";
            if (ieae.hasUnexpectedArguments()) {
                unexpectedMessage = String.format(" Unexpected arguments: %s.", String.join(", ", ieae.getUnexpectedArguments()));
            }
            throw new RuntimeException(String.format("Invalid arguments supplied for link.%s%s", missingMessage, unexpectedMessage));
        }

        restoreFactory.performTimedSync(false, false, false);

        // Sync requests aren't run when executing operations, so stop and check for them after each operation
        for (StackOperation op : endpoint.getStackOperations()) {
            sessionWrapper.executeStackOperations(new Vector<>(Arrays.asList(op)), evalContext);
            Screen s = menuSession.getNextScreen();
            if (s instanceof FormplayerSyncScreen) {
                try {
                    s.init(sessionWrapper);
                    doSyncGetNext((FormplayerSyncScreen) s, menuSession);
                } catch (CommCareSessionException ccse) {
                    throw new RuntimeException("Unable to claim case.");
                }
            }
        }
        menuSessionFactory.rebuildSessionFromFrame(menuSession);
        String[] selections = menuSession.getSelections();

        // reset session and play it back with derived selections
        menuSession.resetSession();
        return advanceSessionWithSelections(menuSession, selections);
    }

    public CaseSearchHelper getCaseSearchHelper() {
        return caseSearchHelper;
    }
}
