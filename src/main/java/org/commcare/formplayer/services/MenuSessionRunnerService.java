package org.commcare.formplayer.services;

import io.sentry.Sentry;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.NotificationMessage;
import org.commcare.formplayer.beans.menus.*;
import org.commcare.formplayer.exceptions.ApplicationConfigException;
import org.commcare.formplayer.objects.QueryData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.suite.model.Text;
import org.commcare.util.screen.*;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import org.commcare.formplayer.objects.FormVolatilityRecord;
import org.commcare.formplayer.repo.MenuSessionRepo;
import org.commcare.formplayer.screens.FormplayerQueryScreen;
import org.commcare.formplayer.screens.FormplayerSyncScreen;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.session.MenuSession;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerDatadog;
import org.commcare.formplayer.util.FormplayerHereFunctionHandler;
import org.commcare.formplayer.util.SessionUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Arrays;

import javax.annotation.Resource;

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
    private QueryRequester queryRequester;

    @Autowired
    private SyncRequester syncRequester;

    @Autowired
    protected FormSessionService formSessionService;

    @Autowired
    protected MenuSessionRepo menuSessionRepo;

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
        return getNextMenu(menuSession, null, 0, "", 0, null);
    }

    private BaseResponseBean getNextMenu(MenuSession menuSession,
                                         String detailSelection,
                                         int offset,
                                         String searchText,
                                         int sortIndex,
                                         QueryData queryData) throws Exception {
        Screen nextScreen = menuSession.getNextScreen();
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
                return getNextMenu(menuSession, detailSelection, offset, searchText, sortIndex, queryData);
            }
            addHereFuncHandler((EntityScreen)nextScreen, menuSession);
            menuResponseBean = new EntityListResponse(
                    (EntityScreen)nextScreen,
                    detailSelection,
                    offset,
                    searchText,
                    sortIndex,
                    storageFactory.getPropertyManager().isFuzzySearchEnabled()
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
        menuResponseBean.setAppVersion(menuSession.getCommCareVersionString() +
                ", App Version: " + menuSession.getAppVersion());
        menuResponseBean.setPersistentCaseTile(getPersistentDetail(menuSession, storageFactory.getPropertyManager().isFuzzySearchEnabled()));
        return menuResponseBean;
    }

    private void addHereFuncHandler(EntityScreen nextScreen, MenuSession menuSession) {
        EvaluationContext ec = nextScreen.getEvalContext();
        ec.addFunctionHandler(new FormplayerHereFunctionHandler(menuSession, menuSession.getCurrentBrowserLocation()));
    }

    public BaseResponseBean advanceSessionWithSelections(MenuSession menuSession,
                                                         String[] selections) throws Exception {
        return advanceSessionWithSelections(menuSession, selections, null, null,
                0, null, 0, false);
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
    public BaseResponseBean advanceSessionWithSelections(MenuSession menuSession,
                                                         String[] selections,
                                                         String detailSelection,
                                                         QueryData queryData,
                                                         int offset,
                                                         String searchText,
                                                         int sortIndex,
                                                         boolean forceManualAction) throws Exception {
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
                    queryData
            );
        }
        NotificationMessage notificationMessage = null;
        for (int i = 1; i <= selections.length; i++) {
            String selection = selections[i - 1];

            boolean confirmed = restoreFactory.isConfirmedSelection(Arrays.copyOfRange(selections, 0, i));

            // minimal entity screens are only safe if there will be no further selection
            // and we do not need the case detail
            needsDetail = detailSelection != null || i != selections.length;
            boolean allowAutoLaunch = i == selections.length;
            boolean gotNextScreen = menuSession.handleInput(selection, needsDetail, confirmed, allowAutoLaunch);
            if (!gotNextScreen) {
                notificationMessage = new NotificationMessage(
                        "Overflowed selections with selection " + selection + " at index " + i,
                        true,
                        NotificationMessage.Tag.selection);
                break;
            }
            Screen nextScreen = menuSession.getNextScreen(needsDetail);

            String queryKey = menuSession.getSessionWrapper().getCommand();
            if (nextScreen instanceof FormplayerQueryScreen) {
                FormplayerQueryScreen formplayerQueryScreen = ((FormplayerQueryScreen)nextScreen);
                formplayerQueryScreen.refreshItemSetChoices();
                boolean autoSearch = formplayerQueryScreen.doDefaultSearch() && !forceManualAction;
                if ((queryData != null && queryData.getExecute(queryKey)) || autoSearch) {
                    notificationMessage = doQuery(
                            (FormplayerQueryScreen)nextScreen,
                            menuSession,
                            queryData == null ? null : queryData.getInputs(queryKey),
                            autoSearch
                    );
                } else if (queryData != null) {
                    answerQueryPrompts((FormplayerQueryScreen)nextScreen,
                            queryData.getInputs(queryKey));
                }
            }
            if (nextScreen instanceof FormplayerSyncScreen) {
                BaseResponseBean syncResponse = doSyncGetNext(
                        (FormplayerSyncScreen)nextScreen,
                        menuSession);
                if (syncResponse != null) {
                    return syncResponse;
                }
            }
        }

        nextResponse = getNextMenu(
                menuSession,
                detailSelection,
                offset,
                searchText,
                sortIndex,
                queryData
        );
        restoreFactory.cacheSessionSelections(selections);

        if (nextResponse != null) {
            if (nextResponse.getNotification() == null && notificationMessage != null) {
                nextResponse.setNotification(notificationMessage);
            }
            log.info("Returning menu: " + nextResponse);
            return nextResponse;
        } else {
            BaseResponseBean responseBean = resolveFormGetNext(menuSession);
            if (responseBean == null) {
                responseBean = new BaseResponseBean(null,
                        new NotificationMessage(null, false, NotificationMessage.Tag.selection),
                        true);
            }
            return responseBean;
        }
    }

    // Sets the query fields and refreshes any itemset choices based on them
    private void answerQueryPrompts(FormplayerQueryScreen screen,
                                    Hashtable<String, String> queryDictionary) {
        if (queryDictionary != null) {
            screen.answerPrompts(queryDictionary);
        }
    }


    /**
     * Perform the sync and update the notification and screen accordingly.
     * After a sync, we can either pop another menu/form to begin
     * or just return to the app menu.
     */
    private BaseResponseBean doSyncGetNext(FormplayerSyncScreen nextScreen,
                                           MenuSession menuSession) throws Exception {
        NotificationMessage notificationMessage = doSync(nextScreen);

        BaseResponseBean postSyncResponse = resolveFormGetNext(menuSession);
        if (postSyncResponse != null) {
            // If not null, we have a form or menu to redirect to
            postSyncResponse.setNotification(notificationMessage);
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
        ResponseEntity<String> responseEntity = syncRequester.makeSyncRequest(screen.getUrl(),
                screen.getBuiltQuery(),
                restoreFactory.getUserHeaders());
        if (responseEntity == null) {
            return new NotificationMessage("Session error, expected sync block but didn't get one.", true, NotificationMessage.Tag.sync);
        }
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            // Don't purge for case claim
            restoreFactory.performTimedSync(false, false);
            return new NotificationMessage("Case claim successful.", false, NotificationMessage.Tag.sync);
        } else {
            return new NotificationMessage(
                    String.format("Case claim failed. Message: %s", responseEntity.getBody()), true, NotificationMessage.Tag.sync);
        }
    }

    /**
     * If we've encountered a QueryScreen and have a QueryDictionary, do the query
     * and update the session, screen, and notification message accordingly.
     * <p>
     * Will do nothing if this wasn't a query screen.
     */
    private NotificationMessage doQuery(FormplayerQueryScreen screen,
                                        MenuSession menuSession,
                                        Hashtable<String, String> queryDictionary,
                                        boolean autoSearch) throws CommCareSessionException {
        log.info("Formplayer doing query with dictionary " + queryDictionary);
        NotificationMessage notificationMessage = null;

        if (queryDictionary != null) {
            screen.answerPrompts(queryDictionary);
        }

        String responseString = queryRequester.makeQueryRequest(screen.getUriString(autoSearch), restoreFactory.getUserHeaders());
        boolean success = screen.processResponse(new ByteArrayInputStream(responseString.getBytes(StandardCharsets.UTF_8)));
        if (success) {
            if (screen.getCurrentMessage() != null) {
                notificationMessage = new NotificationMessage(screen.getCurrentMessage(), false, NotificationMessage.Tag.query);
            }
        } else {
            notificationMessage = new NotificationMessage(
                    "Query failed with message " + screen.getCurrentMessage(),
                    true,
                    NotificationMessage.Tag.query);
        }
        Screen nextScreen = menuSession.getNextScreen();
        log.info("Next screen after query: " + nextScreen);
        return notificationMessage;
    }

    public BaseResponseBean resolveFormGetNext(MenuSession menuSession) throws Exception {
        if (executeAndRebuildSession(menuSession)) {
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

    private NewFormResponse startFormEntry(MenuSession menuSession) throws Exception {
        if (menuSession.getSessionWrapper().getForm() != null) {
            NewFormResponse formResponseBean = generateFormEntrySession(menuSession);
            formResponseBean.setPersistentCaseTile(getPersistentDetail(menuSession, storageFactory.getPropertyManager().isFuzzySearchEnabled()));
            formResponseBean.setBreadcrumbs(menuSession.getBreadcrumbs());
            datadog.addRequestScopedTag(Constants.MODULE_TAG, "form");
            Sentry.setTag(Constants.MODULE_TAG, "form");
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


    private NewFormResponse generateFormEntrySession(MenuSession menuSession) throws Exception {
        menuSessionRepo.save(menuSession.serialize());
        FormSession formEntrySession = menuSession.getFormEntrySession(formSendCalloutHandler, storageFactory);

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
}
