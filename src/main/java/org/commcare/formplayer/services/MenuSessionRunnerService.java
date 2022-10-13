package org.commcare.formplayer.services;

import static org.commcare.formplayer.objects.QueryData.KEY_FORCE_MANUAL_SEARCH;
import static org.commcare.formplayer.util.Constants.TOGGLE_SESSION_ENDPOINTS;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.core.interfaces.RemoteInstanceFetcher;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.beans.auth.FeatureFlagChecker;
import org.commcare.formplayer.beans.menus.BaseResponseBean;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityDetailListResponse;
import org.commcare.formplayer.beans.menus.EntityDetailResponse;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.beans.menus.MenuBean;
import org.commcare.formplayer.beans.menus.QueryResponseBean;
import org.commcare.formplayer.exceptions.ApplicationConfigException;
import org.commcare.formplayer.exceptions.SyncRestoreException;
import org.commcare.formplayer.objects.FormVolatilityRecord;
import org.commcare.formplayer.objects.QueryData;
import org.commcare.formplayer.screens.FormplayerQueryScreen;
import org.commcare.formplayer.screens.FormplayerSyncScreen;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.session.MenuSession;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerDatadog;
import org.commcare.formplayer.util.FormplayerHereFunctionHandler;
import org.commcare.formplayer.web.client.WebClient;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.session.StackObserver;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Endpoint;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.suite.model.StackOperation;
import org.commcare.suite.model.Text;
import org.commcare.util.screen.CommCareSessionException;
import org.commcare.util.screen.EntityScreen;
import org.commcare.util.screen.MenuScreen;
import org.commcare.util.screen.MultiSelectEntityScreen;
import org.commcare.util.screen.QueryScreen;
import org.commcare.util.screen.Screen;
import org.commcare.util.screen.ScreenUtils;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import javax.annotation.Resource;

import datadog.trace.api.Trace;
import io.sentry.Sentry;

/**
 * Class containing logic for accepting a NewSessionRequest and services,
 * restoring the user, opening the new form, and returning the question list response.
 */
@Component
public class MenuSessionRunnerService {

    public static final String NO_SELECTION = "";
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
    protected FormDefinitionService formDefinitionService;

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
        Screen nextScreen = menuSession.getNextScreen(detailSelection != null);

        // No next menu screen? Start form entry!
        if (nextScreen == null) {
            String assertionFailure = getAssertionFailure(menuSession);
            if (assertionFailure != null) {
                BaseResponseBean responseBean = new BaseResponseBean("App Configuration Error",
                        new NotificationMessage(assertionFailure, NotificationMessage.Type.app_error,
                                NotificationMessage.Tag.menu),
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
                return getNextMenu(menuSession, detailSelection, offset, searchText, sortIndex, queryData,
                        casesPerPage, smartLinkTemplate);
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
            String caseListName = ScreenUtils.getBestTitle(menuSession.getSessionWrapper());
            datadog.addRequestScopedTag(Constants.MODULE_NAME_TAG, caseListName);
            Sentry.setTag(Constants.MODULE_NAME_TAG, caseListName);
        } else if (nextScreen instanceof FormplayerQueryScreen) {
            String queryKey = menuSession.getSessionWrapper().getCommand();
            if (queryData != null) {
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
        menuResponseBean.setAppVersion(
                menuSession.getCommCareVersionString() + ", App Version: " + menuSession.getAppVersion());
        menuResponseBean.setPersistentCaseTile(
                getPersistentDetail(menuSession, storageFactory.getPropertyManager().isFuzzySearchEnabled()));
        return menuResponseBean;
    }

    private void addHereFuncHandler(EntityScreen nextScreen, MenuSession menuSession) {
        EvaluationContext ec = nextScreen.getEvalContext();
        ec.addFunctionHandler(
                new FormplayerHereFunctionHandler(menuSession, menuSession.getCurrentBrowserLocation()));
    }

    @Trace
    public BaseResponseBean advanceSessionWithSelections(MenuSession menuSession,
            String[] selections, QueryData queryData) throws Exception {
        return advanceSessionWithSelections(menuSession, selections, null, queryData,
                0, null, 0, false, 0, null, null);
    }

    /**
     * Advances the session based on the selections.
     *
     * @param selections      - Selections are either an integer index into a list of modules
     *                        or a case id indicating the case selected for a case detail.
     *                        <p>
     *                        An example selection would be ["0", "2", "6c5d91e9-61a2-4264-97f3-5d68636ff316"]
     *                        <p>
     *                        This would mean select the 0th menu, then the 2nd menu, then the case with the id
     *                        6c5d91e9-61a2-4264-97f3-5d68636ff316.
     * @param detailSelection - If requesting a case detail will be a case id, else null. When the case id is given
     *                        it is used to short circuit the normal TreeReference calculation by inserting a
     *                        predicate that
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
            String smartLinkTemplate,
            String[] selectedValues) throws Exception {
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
        for (int i = 1; i <= selections.length; i++) {
            String selection = selections[i - 1];

            boolean inputValidated = restoreFactory.isConfirmedSelection(Arrays.copyOfRange(selections, 0, i));
            boolean isDetailScreen = detailSelection != null;

            // minimal entity screens are only safe if there will be no further selection
            // and we do not need the case detail
            boolean needsFullEntityScreen = isDetailScreen || i != selections.length;
            boolean gotNextScreen = menuSession.handleInput(selection, needsFullEntityScreen, inputValidated,
                    true, selectedValues, isDetailScreen);
            if (!gotNextScreen) {
                notificationMessage = new NotificationMessage(
                        "Overflowed selections with selection " + selection + " at index " + i,
                        true,
                        NotificationMessage.Tag.selection);
                break;
            }
            String nextInput = i == selections.length ? NO_SELECTION : selections[i];
            Screen nextScreen;
            try {
                nextScreen = autoAdvanceSession(menuSession, selection, nextInput, queryData,
                        needsFullEntityScreen, inputValidated, forceManualAction, isDetailScreen);
            } catch (CommCareSessionException e) {
                notificationMessage = new NotificationMessage(e.getMessage(), true, NotificationMessage.Tag.query);
                break;
            }

            if (nextScreen == null && menuSession.getSessionWrapper().getForm() == null) {
                // we've reached the end of this navigation path and no form in sight
                // this usually means a RemoteRequestEntry was involved
                if (nextInput != NO_SELECTION) {
                    // still more nav to do so rebuild the session and continue
                    executeAndRebuildSession(menuSession);
                } else {
                    // no more nav, we're done
                    BaseResponseBean postSyncResponse = resolveFormGetNext(menuSession);
                    if (postSyncResponse == null) {
                        // Return use to the app root
                        postSyncResponse = new BaseResponseBean(null,
                                new NotificationMessage("Redirecting after sync", false,
                                        NotificationMessage.Tag.sync),
                                true);
                    }
                    return postSyncResponse;
                }
            } else {
                if (!selection.contentEquals(MultiSelectEntityScreen.USE_SELECTED_VALUES)) {
                    menuSession.addSelection(selection);
                }
            }
        }

        BaseResponseBean nextResponse = getNextMenu(
                menuSession,
                detailSelection,
                offset,
                searchText,
                sortIndex,
                queryData,
                casesPerPage,
                smartLinkTemplate
        );
        restoreFactory.cacheSessionSelections(menuSession.getSelections());

        if (nextResponse != null) {
            if (nextResponse.getNotification() == null && notificationMessage != null) {
                nextResponse.setNotification(notificationMessage);
            }
            log.info("Returning menu: " + nextResponse);
            nextResponse.setSelections(menuSession.getSelections());
            return nextResponse;
        } else {
            if (notificationMessage == null) {
                notificationMessage = new NotificationMessage(null, false, NotificationMessage.Tag.selection);
            }
            return new BaseResponseBean(null, notificationMessage, true);
        }
    }

    /**
     * Apply an actions to the menu session that do not require user input e.g.
     * - auto launch
     * - queries
     * - auto advance menu
     *
     * @param menuSession
     * @param currentInput          The current input being processed
     * @param nextInput             The next input being processed or NO_SELECTION constant
     * @param queryData             Query data from the request
     * @param needsFullEntityScreen Whether the full entity screen is required
     * @param inputValidated        Whether the input has been validated (allows skipping validation)
     * @param forceManualAction     Prevent auto execution of queries if true.
     * @param isDetailScreen        Whether the current request is for an Entity Detail Screen
     * @return
     * @throws CommCareSessionException
     */
    private Screen autoAdvanceSession(
            MenuSession menuSession,
            String currentInput,
            String nextInput,
            QueryData queryData,
            boolean needsFullEntityScreen,
            boolean inputValidated,
            boolean forceManualAction,
            boolean isDetailScreen) throws CommCareSessionException {
        boolean sessionAdvanced;
        Screen nextScreen = null;
        Screen previousScreen;
        int iterationCount = 0;
        int maxIterations = 50; // maximum plausible iterations
        do {
            sessionAdvanced = false;
            previousScreen = nextScreen;
            iterationCount += 1;

            nextScreen = menuSession.getNextScreen(needsFullEntityScreen, isDetailScreen);
            if (previousScreen != null) {
                String to = nextScreen == null ? "XForm" : nextScreen.toString();
                log.info(String.format("Menu session auto advanced from %s to %s", previousScreen, to));
            }

            if (nextScreen instanceof EntityScreen) {
                // Advance the session in case auto launch is set
                sessionAdvanced = handleAutoLaunch((EntityScreen)nextScreen, menuSession, currentInput,
                        needsFullEntityScreen, inputValidated, nextInput, isDetailScreen);
            } else if (nextScreen instanceof FormplayerQueryScreen) {
                boolean replay = !nextInput.equals(NO_SELECTION);
                boolean skipCache = !(replay || isDetailScreen);
                sessionAdvanced = handleQueryScreen(
                        (FormplayerQueryScreen)nextScreen, menuSession, queryData,
                        replay, forceManualAction, skipCache
                );
            } else if (nextScreen instanceof MenuScreen) {
                sessionAdvanced = menuSession.autoAdvanceMenu(nextScreen, isAutoAdvanceMenu());
            } else if (nextScreen instanceof FormplayerSyncScreen) {
                try {
                    doPostAndSync(menuSession, (FormplayerSyncScreen) nextScreen);
                } catch (SyncRestoreException e) {
                    throw new CommCareSessionException(e.getMessage(), e);
                }
                sessionAdvanced = true;
            }
        } while (!Thread.interrupted() && sessionAdvanced && iterationCount < maxIterations);

        return nextScreen;
    }

    /**
     * This method handles Query Screens during app navigation. This method does nothing
     * if the 'nextScreen' is not a query screen.
     *
     * @param queryScreen       The query screen to handle
     * @param menuSession       The current menu session
     * @param queryData         Query data passed in from the response
     * @param replay            Boolean that is True if there are still more selections to process in the
     *                          navigation loop.
     *                          i.e. if we are handling the query as part of navigation replay
     * @param forceManualAction Boolean passed in from the request which will prevent auto launch actions
     * @return true if the query was executed and the session should move to the next screen
     * @throws CommCareSessionException if the was an error performing a query
     */
    private boolean handleQueryScreen(FormplayerQueryScreen queryScreen, MenuSession menuSession,
            QueryData queryData,
            boolean replay, boolean forceManualAction, boolean skipCache)
            throws CommCareSessionException {
        String queryKey = menuSession.getSessionWrapper().getCommand();
        boolean forceManualSearch = queryData != null && queryData.isForceManualSearch(queryKey);

        // this is to manintain backward compatibility with forceManualAction flag,
        // to be removed soon after a deploy cycle
        if (queryData == null || !queryData.hasProperty(queryKey, KEY_FORCE_MANUAL_SEARCH)) {
            forceManualSearch = forceManualAction;
        }

        boolean autoSearch = replay || (queryScreen.doDefaultSearch() && !forceManualSearch);
        if ((queryData != null && queryData.getExecute(queryKey)) || autoSearch) {
            return doQuery(
                    queryScreen,
                    queryData == null ? null : queryData.getInputs(queryKey),
                    queryScreen.doDefaultSearch() && !forceManualSearch,
                    skipCache
            );
        } else if (queryData != null) {
            answerQueryPrompts(queryScreen, queryData.getInputs(queryKey));
            return false;
        }
        return false;
    }

    private boolean isAutoAdvanceMenu() {
        return storageFactory.getPropertyManager().isAutoAdvanceMenu();
    }

    /**
     * Handle auto-launch actions for EntityScreens
     *
     * @return true if the session was advanced
     * @throws CommCareSessionException
     */
    private boolean handleAutoLaunch(EntityScreen entityScreen, MenuSession menuSession, String selection,
            boolean needsFullEntityScreen, boolean inputValidated, String nextInput, boolean isDetailScreen)
            throws CommCareSessionException {
        entityScreen.evaluateAutoLaunch(nextInput);
        if (entityScreen.getAutoLaunchAction() != null) {
            menuSession.handleInput(selection, needsFullEntityScreen, inputValidated, true, null, isDetailScreen);
            return true;
        }
        return false;
    }

    // Sets the query fields and refreshes any itemset choices based on them
    private void answerQueryPrompts(FormplayerQueryScreen screen,
            Hashtable<String, String> queryDictionary) {
        if (queryDictionary != null) {
            screen.answerPrompts(queryDictionary);
        }
    }

    /**
     * Execute the post request associated with the sync screen and perform a sync if necessary.
     */
    private void doPostAndSync(MenuSession menuSession, FormplayerSyncScreen screen) throws SyncRestoreException {
        Boolean shouldSync;
        try {
            shouldSync = webClient.caseClaimPost(screen.getUrl(), screen.getQueryParams());
            screen.updateSessionOnSuccess();
        } catch (RestClientResponseException e) {
            throw new SyncRestoreException(
                    String.format("Case claim failed. Message: %s", e.getResponseBodyAsString()), e);
        } catch (RestClientException e) {
            throw new SyncRestoreException("Unknown error performing case claim", e);
        }
        if (shouldSync) {
            restoreFactory.performTimedSync(false, true, false);
            menuSession.getSessionWrapper().clearVolatiles();
        }
    }

    /**
     * If we've encountered a QueryScreen and have a QueryDictionary, do the query
     * and update the session, screen, and notification message accordingly.
     * <p>
     * Will do nothing if this wasn't a query screen.
     */
    @Trace
    private boolean doQuery(FormplayerQueryScreen screen,
            Hashtable<String, String> queryDictionary,
            boolean isDefaultSearch, boolean skipCache) throws CommCareSessionException {
        log.info("Formplayer doing query with dictionary " + queryDictionary);
        answerQueryPrompts(screen, queryDictionary);

        // Only search when there are no errors in input or we are doing a default search
        if (isDefaultSearch || screen.getErrors().isEmpty()) {
            try {
                ExternalDataInstance searchDataInstance = caseSearchHelper.getRemoteDataInstance(
                        screen.getQueryDatum().getDataId(),
                        screen.getQueryDatum().useCaseTemplate(),
                        screen.getBaseUrl(),
                        screen.getRequestData(isDefaultSearch),
                        skipCache);
                screen.updateSession(searchDataInstance);
                return true;
            } catch (InvalidStructureException | IOException
                    | XmlPullParserException | UnfullfilledRequirementsException e) {
                throw new CommCareSessionException("Query response format error: " + e.getMessage(), e);
            }
        }
        return false;
    }

    @Trace
    public BaseResponseBean resolveFormGetNext(MenuSession menuSession) throws Exception {
        if (executeAndRebuildSession(menuSession)) {
            if (menuSession.getSmartLinkRedirect() != null) {
                BaseResponseBean responseBean = new BaseResponseBean(null, null, true);
                UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(
                        menuSession.getSmartLinkRedirect());
                responseBean.setSmartLinkRedirect(urlBuilder.build().toString());
                return responseBean;
            }

            autoAdvanceSession(menuSession, "", "", new QueryData(),
                    false, false, false, false
            );
            BaseResponseBean response = getNextMenu(menuSession);
            response.setSelections(menuSession.getSelections());
            return response;
        }
        return null;
    }

    // Rebuild the session after executing any pending session stack
    private boolean executeAndRebuildSession(MenuSession menuSession)
            throws CommCareSessionException, RemoteInstanceFetcher.RemoteInstanceException {
        menuSession.getSessionWrapper().syncState();
        StackObserver observer = new StackObserver();
        EvaluationContext ec = menuSession.getSessionWrapper().getEvaluationContext();
        boolean continueSession = menuSession.getSessionWrapper().finishExecuteAndPop(ec, observer);
        clearVolatiles(menuSession.getSessionWrapper(), observer);
        if (continueSession) {
            String smartLinkRedirect = menuSession.getSessionWrapper().getSmartLinkRedirect();
            if (smartLinkRedirect != null) {
                menuSession.setSmartLinkRedirect(smartLinkRedirect);
            } else {
                menuSessionFactory.rebuildSessionFromFrame(menuSession, caseSearchHelper);
            }
            return true;
        }
        return false;
    }

    private void clearVolatiles(SessionWrapper session, StackObserver observer) {
        session.clearVolatiles();
        for (StackFrameStep abandonedStep : observer.getRemovedSteps()) {
            for (ExternalDataInstanceSource source : abandonedStep.getDataInstanceSources().values()) {
                try {
                    caseSearchHelper.clearCacheForInstanceSource(source);
                } catch (InvalidStructureException e) {
                    log.warn("Error clearing remote instance cache");
                }
            }
        }
    }

    protected static TreeReference getReference(SessionWrapper session, EntityDatum entityDatum) {
        EvaluationContext ec = session.getEvaluationContext();
        StackFrameStep stepToFrame = getStepToFrame(session);
        String caseId = stepToFrame.getValue();
        TreeReference reference = entityDatum.getEntityFromID(ec, caseId);
        if (reference == null) {
            throw new ApplicationConfigException(String.format("Could not create tile for case with ID %s " +
                            "because this case does not meet the criteria for the case list with ID %s.", caseId,
                    entityDatum.getShortDetail()));
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

    private static EntityDetailListResponse getDetail(MenuSession menuSession, boolean inline,
            boolean isFuzzySearchEnabled) {
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
            return new EntityDetailListResponse(persistentDetail.getFlattenedDetails(), ec, reference,
                    isFuzzySearchEnabled);
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
            formResponseBean.setAppVersion(
                    menuSession.getCommCareVersionString() + ", App Version: " + menuSession.getAppVersion());
            formResponseBean.setPersistentCaseTile(
                    getPersistentDetail(menuSession, storageFactory.getPropertyManager().isFuzzySearchEnabled()));
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
        Text text = menuSession.getSessionWrapper().getCurrentEntry().getAssertions().getAssertionFailure(
                menuSession.getEvalContextWithHereFuncHandler());
        if (text != null) {
            return text.evaluate(menuSession.getEvalContextWithHereFuncHandler());
        }
        return null;
    }

    @Trace
    private NewFormResponse generateFormEntrySession(MenuSession menuSession) throws Exception {
        menuSessionService.saveSession(menuSession.serialize());
        FormSession formEntrySession = menuSession.getFormEntrySession(formSendCalloutHandler, storageFactory,
                formDefinitionService);

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

    public BaseResponseBean advanceSessionWithEndpoint(MenuSession menuSession, String endpointId,
            @Nullable HashMap<String, String> endpointArgs)
            throws Exception {
        if (!FeatureFlagChecker.isToggleEnabled(TOGGLE_SESSION_ENDPOINTS)) {
            throw new RuntimeException("Linking into applications has been disabled for this project.");
        }

        Endpoint endpoint = menuSession.getEndpoint(endpointId);
        if (endpoint == null) {
            throw new RuntimeException(
                    "This link does not exist. Your app may have changed so that the given link is no longer "
                            + "valid");
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
                missingMessage = String.format(" Missing arguments: %s.",
                        String.join(", ", ieae.getMissingArguments()));
            }
            String unexpectedMessage = "";
            if (ieae.hasUnexpectedArguments()) {
                unexpectedMessage = String.format(" Unexpected arguments: %s.",
                        String.join(", ", ieae.getUnexpectedArguments()));
            }
            throw new RuntimeException(
                    String.format("Invalid arguments supplied for link.%s%s", missingMessage, unexpectedMessage));
        }

        // Sync requests aren't run when executing operations, so stop and check for them after each operation
        for (StackOperation op : endpoint.getStackOperations()) {
            sessionWrapper.executeStackOperations(new Vector<>(Arrays.asList(op)), evalContext);
            Screen screen = menuSession.getNextScreen(false);
            if (screen instanceof FormplayerSyncScreen) {
                try {
                    screen.init(sessionWrapper);
                    doPostAndSync(menuSession, (FormplayerSyncScreen)screen);
                    executeAndRebuildSession(menuSession);
                } catch (CommCareSessionException ccse) {
                    throw new RuntimeException("Unable to claim case.");
                }
            }
        }
        menuSessionFactory.rebuildSessionFromFrame(menuSession, caseSearchHelper);
        String[] selections = menuSession.getSelections();

        // reset session and play it back with derived selections
        menuSession.resetSession();
        return advanceSessionWithSelections(menuSession, selections, null);
    }

    public CaseSearchHelper getCaseSearchHelper() {
        return caseSearchHelper;
    }
}
