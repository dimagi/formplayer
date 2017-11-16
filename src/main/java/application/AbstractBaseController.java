package application;

import aspects.LockAspect;
import beans.InstallRequestBean;
import beans.NewFormResponse;
import beans.NotificationMessage;
import beans.SessionNavigationBean;
import beans.exceptions.ExceptionResponseBean;
import beans.exceptions.HTMLExceptionResponseBean;
import beans.exceptions.RetryExceptionResponseBean;
import beans.menus.*;
import com.timgroup.statsd.StatsDClient;
import exceptions.*;
import io.sentry.event.Event;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.modern.models.RecordTooLargeException;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.util.screen.*;
import org.commcare.util.screen.MenuScreen;
import org.commcare.util.screen.Screen;
import org.javarosa.core.model.actions.FormSendCalloutHandler;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import repo.FormSessionRepo;
import repo.MenuSessionRepo;
import repo.SerializableMenuSession;
import repo.impl.PostgresUserRepo;
import screens.FormplayerQueryScreen;
import screens.FormplayerSyncScreen;
import services.*;
import session.FormSession;
import session.MenuSession;
import util.Constants;
import util.FormplayerHttpRequest;
import util.FormplayerSentry;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Base Controller class containing exception handling logic and
 * autowired beans used in both MenuController and FormController
 */
public abstract class AbstractBaseController {

    @Value("${commcarehq.host}")
    protected String host;

    @Autowired
    private QueryRequester queryRequester;

    @Autowired
    private SyncRequester syncRequester;

    @Autowired
    protected FormSessionRepo formSessionRepo;

    @Autowired
    protected MenuSessionRepo menuSessionRepo;

    @Autowired
    protected InstallService installService;

    @Autowired
    protected RestoreFactory restoreFactory;

    @Autowired
    protected NewFormResponseFactory newFormResponseFactory;

    @Autowired
    protected FormplayerStorageFactory storageFactory;

    @Autowired
    protected StatsDClient datadogStatsDClient;

    @Autowired
    protected FormplayerSentry raven;

    @Autowired
    protected FormSendCalloutHandler formSendCalloutHandler;

    @Autowired
    PostgresUserRepo postgresUserRepo;

    @Value("${commcarehq.host}")
    private String hqHost;

    @Value("${commcarehq.environment}")
    private String hqEnvironment;

    private final Log log = LogFactory.getLog(AbstractBaseController.class);


    public BaseResponseBean resolveFormGetNext(MenuSession menuSession) throws Exception {
        menuSession.getSessionWrapper().syncState();
        if(menuSession.getSessionWrapper().finishExecuteAndPop(menuSession.getSessionWrapper().getEvaluationContext())){
            menuSession.getSessionWrapper().clearVolatiles();
            MenuSession newMenuSession = menuSession.rebuildSessionFromFrame(menuSession.getSessionWrapper().getFrame(), installService, restoreFactory, host);
            BaseResponseBean response = getNextMenu(newMenuSession);
            response.setSelections(menuSession.getSelections());
            return response;
        }
        return null;
    }

    public BaseResponseBean getNextMenu(MenuSession menuSession) throws Exception {
        return getNextMenu(menuSession, 0, "", 0);
    }

    protected BaseResponseBean getNextMenu(MenuSession menuSession,
                                           int offset,
                                           String searchText,
                                           int sortIndex) throws Exception {
        Screen nextScreen = menuSession.getNextScreen();
        // If the nextScreen is null, that means we are heading into
        // form entry and there isn't a screen title
        if (nextScreen == null) {
            return getNextMenu(menuSession, null, offset, searchText, sortIndex);
        }
        return getNextMenu(menuSession, null, offset, searchText, sortIndex);
    }

    protected BaseResponseBean getNextMenu(MenuSession menuSession,
                                           String detailSelection,
                                           int offset,
                                           String searchText,
                                           int sortIndex) throws Exception {
        Screen nextScreen;

        // If we were redrawing, remain on the current screen. Otherwise, advance to the next.
        nextScreen = menuSession.getNextScreen();
        // No next menu screen? Start form entry!
        if (nextScreen == null) {
            if(menuSession.getSessionWrapper().getForm() != null) {
                NewFormResponse formResponseBean = generateFormEntryScreen(menuSession);
                setPersistentCaseTile(menuSession, formResponseBean);
                formResponseBean.setBreadcrumbs(menuSession.getTitles());
                return formResponseBean;
            } else{
                return null;
            }
        } else {
            MenuBean menuResponseBean;

            // We're looking at a module or form menu
            if (nextScreen instanceof MenuScreen) {
                menuResponseBean = generateMenuScreen((MenuScreen) nextScreen, menuSession.getSessionWrapper(),
                        menuSession.getId());
            }
            // We're looking at a case list or detail screen
            else if (nextScreen instanceof EntityScreen) {
                menuResponseBean = generateEntityScreen(
                        (EntityScreen) nextScreen,
                        detailSelection,
                        offset,
                        searchText,
                        menuSession.getId(),
                        sortIndex
                );
            } else if(nextScreen instanceof FormplayerQueryScreen){
                    menuResponseBean = generateQueryScreen((QueryScreen) nextScreen, menuSession.getSessionWrapper());
            } else {
                throw new Exception("Unable to recognize next screen: " + nextScreen);
            }
            menuResponseBean.setBreadcrumbs(menuSession.getTitles());
            menuResponseBean.setAppId(menuSession.getAppId());
            menuResponseBean.setAppVersion(menuSession.getCommCareVersionString() +
                    ", App Version: " + menuSession.getAppVersion());
            setPersistentCaseTile(menuSession, menuResponseBean);
            return menuResponseBean;
        }
    }

    protected StackFrameStep getStepToFrame(SessionWrapper session) {
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

    protected TreeReference getReference(SessionWrapper session, EntityDatum entityDatum) {
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

    protected EntityDetailListResponse getInlineDetail(MenuSession menuSession) {
        return getDetail(menuSession, true);
    }

    protected EntityDetailResponse getPersistentDetail(MenuSession menuSession) {
        EntityDetailListResponse detailListResponse = getDetail(menuSession, false);
        if (detailListResponse == null) {
            return null;
        }
        EntityDetailResponse[] detailList = detailListResponse.getEntityDetailList();
        if (detailList == null) {
            return null;
        }
        return detailList[0];
    }

    protected EntityDetailListResponse getDetail(MenuSession menuSession, boolean inline) {
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
            ec = session.getEvaluationContext();
            return new EntityDetailListResponse(persistentDetail.getFlattenedDetails(),
                    ec,
                    reference);
        } else {
            ec = new EvaluationContext(menuSession.getSessionWrapper().getEvaluationContext(), reference);
            EntityDetailResponse detailResponse = new EntityDetailResponse(persistentDetail, ec);
            detailResponse.setHasInlineTile(entityDatum.getInlineDetail() != null);
            return new EntityDetailListResponse(detailResponse);
        }

    }

    private void setPersistentCaseTile(MenuSession menuSession, NewFormResponse formResponse) {
        formResponse.setPersistentCaseTile(getPersistentDetail(menuSession));
    }

    private void setPersistentCaseTile(MenuSession menuSession, MenuBean menuResponseBean) {
        menuResponseBean.setPersistentCaseTile(getPersistentDetail(menuSession));
    }

    private QueryResponseBean generateQueryScreen(QueryScreen nextScreen, SessionWrapper sessionWrapper) {
        return new QueryResponseBean(nextScreen, sessionWrapper);
    }

    private CommandListResponseBean generateMenuScreen(MenuScreen nextScreen, SessionWrapper session,
                                                       String menuSessionId) {
        return new CommandListResponseBean(nextScreen, session, menuSessionId);
    }

    private EntityListResponse generateEntityScreen(EntityScreen nextScreen, String detailSelection, int offset, String searchText,
                                                    String menuSessionId, int sortIndex) {
        return new EntityListResponse(nextScreen, detailSelection, offset, searchText, menuSessionId, sortIndex);
    }

    private NewFormResponse generateFormEntryScreen(MenuSession menuSession) throws Exception {
        FormSession formEntrySession = menuSession.getFormEntrySession(formSendCalloutHandler);
        menuSessionRepo.save(new SerializableMenuSession(menuSession));
        NewFormResponse response = new NewFormResponse(formEntrySession);
        formSessionRepo.save(formEntrySession.serialize());
        return response;
    }

    protected MenuSession getMenuSession(String menuSessionId) throws Exception {
        MenuSession menuSession = new MenuSession(
                menuSessionRepo.findOneWrapped(menuSessionId),
                installService,
                restoreFactory,
                host
        );
        menuSession.getSessionWrapper().syncState();
        return menuSession;
    }

    /**
     * Catch all the exceptions that we *do not* want emailed here
     */
    @ExceptionHandler({ApplicationConfigException.class,
            XPathException.class,
            CommCareInstanceInitializer.FixtureInitializationException.class,
            CommCareSessionException.class,
            FormNotFoundException.class,
            RecordTooLargeException.class,
            InvalidStructureException.class,
            UnresolvedResourceRuntimeException.class})
    @ResponseBody
    public ExceptionResponseBean handleApplicationError(FormplayerHttpRequest request, Exception exception) {
        log.error("Request: " + request.getRequestURL() + " raised " + exception);
        incrementDatadogCounter(Constants.DATADOG_ERRORS_APP_CONFIG, request);
        raven.sendRavenException(exception, Event.Level.INFO);
        return getPrettyExceptionResponse(exception, request);
    }

    private ExceptionResponseBean getPrettyExceptionResponse(Exception exception, FormplayerHttpRequest request) {
        String message = exception.getMessage();
        if (exception instanceof XPathTypeMismatchException && message.contains("instance(groups)")) {
            message = "The case sharing settings for your user are incorrect. " +
                    "This user must be in exactly one case sharing group. " +
                    "Please contact your supervisor.";
        }
        return new ExceptionResponseBean(message, request.getRequestURL().toString());
    }

    /**
     * Handles exceptions thrown when making external requests, usually to CommCareHQ.
     */
    @ExceptionHandler({HttpClientErrorException.class})
    @ResponseBody
    public ExceptionResponseBean handleHttpRequestError(FormplayerHttpRequest req, HttpClientErrorException exception) {
        incrementDatadogCounter(Constants.DATADOG_ERRORS_EXTERNAL_REQUEST, req);
        log.error(String.format("Exception %s making external request %s.", exception, req));
        return new ExceptionResponseBean(exception.getResponseBodyAsString(), req.getRequestURL().toString());
    }

    @ExceptionHandler({AsyncRetryException.class})
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @ResponseBody
    public RetryExceptionResponseBean handleAsyncRetryException(FormplayerHttpRequest req, AsyncRetryException exception) {
        return new RetryExceptionResponseBean(
                exception.getMessage(),
                req.getRequestURL().toString(),
                exception.getDone(),
                exception.getTotal(),
                exception.getRetryAfter()
        );
    }
    /**
     * Catch exceptions that have formatted HTML errors
     */
    @ExceptionHandler({FormattedApplicationConfigException.class})
    @ResponseBody
    public HTMLExceptionResponseBean handleFormattedApplicationError(FormplayerHttpRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception);
        incrementDatadogCounter(Constants.DATADOG_ERRORS_APP_CONFIG, req);

        return new HTMLExceptionResponseBean(exception.getMessage(), req.getRequestURL().toString());
    }

    @ExceptionHandler({LockAspect.LockError.class})
    @ResponseBody
    @ResponseStatus(HttpStatus.LOCKED)
    public ExceptionResponseBean handleLockError(FormplayerHttpRequest req, Exception exception) {
        return new ExceptionResponseBean("User lock timed out", req.getRequestURL().toString());
    }

    @ExceptionHandler({InterruptedRuntimeException.class})
    @ResponseBody
    public ExceptionResponseBean handleInterruptException(FormplayerHttpRequest req, Exception exception) {
        return new ExceptionResponseBean("An issue prevented us from processing your previous action, please try again",
                req.getRequestURL().toString());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ExceptionResponseBean handleError(FormplayerHttpRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception);
        incrementDatadogCounter(Constants.DATADOG_ERRORS_CRASH, req);
        exception.printStackTrace();
        raven.sendRavenException(exception);
        if (exception instanceof ClientAbortException) {
            // We can't actually return anything since the client has bailed. To avoid errors return null
            // https://mtyurt.net/2016/04/18/spring-how-to-handle-ioexception-broken-pipe/
            log.error("Client Aborted! Returning null");
            return null;
        }
        return new ExceptionResponseBean(exception.getMessage(), req.getRequestURL().toString());
    }

    private void incrementDatadogCounter(String metric, FormplayerHttpRequest req) {
        String user = "unknown";
        String domain = "unknown";
        if (req.getUserDetails() != null) {
            user = req.getUserDetails().getUsername();
        }
        if (req.getDomain() != null) {
            domain = req.getDomain();
        }
        datadogStatsDClient.increment(
                metric,
                "domain:" + domain,
                "user:" + user,
                "request:" + req.getRequestURI()
        );
    }

    protected BaseResponseBean advanceSessionWithSelections(MenuSession menuSession,
                                                            String[] selections) throws Exception {
        return advanceSessionWithSelections(menuSession, selections, null, null, 0, null, 0);
    }

    /**
     * Advances the session based on the selections.
     *
     * @param menuSession
     * @param selections      - Selections are either an integer index into a list of modules
     *                        or a case id indicating the case selected for a case detail.
     *                        <p>
     *                        An example selection would be ["0", "2", "6c5d91e9-61a2-4264-97f3-5d68636ff316"]
     *                        <p>
     *                        This would mean select the 0th menu, then the 2nd menu, then the case with the id 6c5d91e9-61a2-4264-97f3-5d68636ff316.
     * @param detailSelection - If requesting a case detail will be a case id, else null. When the case id is given
     *                        it is used to short circuit the normal TreeReference calculation by inserting a predicate that
     *                        is [@case_id = <detailSelection>].
     * @param queryDictionary
     * @param offset
     * @param searchText
     */
    protected BaseResponseBean advanceSessionWithSelections(MenuSession menuSession,
                                                          String[] selections,
                                                          String detailSelection,
                                                          Hashtable<String, String> queryDictionary,
                                                          int offset,
                                                          String searchText,
                                                          int sortIndex) throws Exception {
        BaseResponseBean nextMenu;
        // If we have no selections, we're are the root screen.
        if (selections == null) {
            nextMenu = getNextMenu(
                    menuSession,
                    offset,
                    searchText,
                    sortIndex
            );
            return nextMenu;
        }

        String[] overrideSelections = null;
        NotificationMessage notificationMessage = new NotificationMessage();
        for (int i = 1; i <= selections.length; i++) {
            String selection = selections[i - 1];
            boolean gotNextScreen = menuSession.handleInput(selection);
            if (!gotNextScreen) {
                notificationMessage = new NotificationMessage(
                        "Overflowed selections with selection " + selection + " at index " + i, (true));
                break;
            }
            Screen nextScreen = menuSession.getNextScreen();

            if (nextScreen instanceof FormplayerQueryScreen && queryDictionary != null) {
                notificationMessage = doQuery(
                        (FormplayerQueryScreen) nextScreen,
                        menuSession,
                        queryDictionary
                );
                overrideSelections = trimCaseClaimSelections(selections);
            }
            if (nextScreen instanceof FormplayerSyncScreen) {
                BaseResponseBean syncResponse = doSyncGetNext(
                        (FormplayerSyncScreen) nextScreen,
                        menuSession);
                if (syncResponse != null) {
                    syncResponse.setSelections(overrideSelections);
                    return syncResponse;
                }
            }
        }
        nextMenu = getNextMenu(
                menuSession,
                detailSelection,
                offset,
                searchText,
                sortIndex
        );
        if (nextMenu != null) {
            nextMenu.setNotification(notificationMessage);
            nextMenu.setSelections(overrideSelections);
            log.info("Returning menu: " + nextMenu);
            return nextMenu;
        } else {
            BaseResponseBean responseBean = resolveFormGetNext(menuSession);
            if (responseBean == null) {
                responseBean = new BaseResponseBean(null, "Got null menu, redirecting to home screen.", false, true);
            }
            responseBean.setSelections(overrideSelections);
            return responseBean;
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
            postSyncResponse = new BaseResponseBean(null, "Redirecting after sync", false, true);
            postSyncResponse.setNotification(notificationMessage);
            return postSyncResponse;
        }
    }

    private NotificationMessage doSync(FormplayerSyncScreen screen) throws Exception {
        ResponseEntity<String> responseEntity = syncRequester.makeSyncRequest(screen.getUrl(),
                screen.getBuiltQuery(),
                restoreFactory.getUserHeaders());
        if (responseEntity == null) {
            return new NotificationMessage("Session error, expected sync block but didn't get one.", true);
        }
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            restoreFactory.performTimedSync();
            return new NotificationMessage("Case claim successful.", false);
        } else {
            return new NotificationMessage(
                    String.format("Case claim failed. Message: %s", responseEntity.getBody()), true);
        }
    }

    private String[] trimCaseClaimSelections(String[] selections) {
        String actionSelections = selections[selections.length - 2];
        if (!actionSelections.contains("action")) {
            log.error(String.format("Selections %s did not contain expected action at position %s.",
                    Arrays.toString(selections),
                    selections[selections.length - 2]));
            return selections;
        }
        String[] newSelections = new String[selections.length - 1];
        System.arraycopy(selections, 0, newSelections, 0, selections.length - 2);
        newSelections[selections.length - 2] = selections[selections.length - 1];
        return newSelections;
    }

    /**
     * If we've encountered a QueryScreen and have a QueryDictionary, do the query
     * and update the session, screen, and notification message accordingly.
     * <p>
     * Will do nothing if this wasn't a query screen.
     */
    private NotificationMessage doQuery(FormplayerQueryScreen screen,
                                        MenuSession menuSession,
                                        Hashtable<String, String> queryDictionary) throws CommCareSessionException {
        log.info("Formplayer doing query with dictionary " + queryDictionary);
        NotificationMessage notificationMessage = null;
        screen.answerPrompts(queryDictionary);
        String responseString = queryRequester.makeQueryRequest(screen.getUriString(), restoreFactory.getUserHeaders());
        boolean success = screen.processSuccess(new ByteArrayInputStream(responseString.getBytes(StandardCharsets.UTF_8)));
        if (success) {
            if (screen.getCurrentMessage() != null) {
                notificationMessage = new NotificationMessage(screen.getCurrentMessage(), false);
            }
        } else {
            notificationMessage = new NotificationMessage("Query failed with message " + screen.getCurrentMessage(), true);
        }
        Screen nextScreen = menuSession.getNextScreen();
        log.info("Next screen after query: " + nextScreen);
        return notificationMessage;
    }

    protected MenuSession getMenuSessionFromBean(SessionNavigationBean sessionNavigationBean) throws Exception {
        return performInstall(sessionNavigationBean);
    }

    protected MenuSession performInstall(InstallRequestBean bean) throws Exception {
        if ((bean.getAppId() == null || "".equals(bean.getAppId())) &&
                bean.getInstallReference() == null || "".equals(bean.getInstallReference())) {
            throw new RuntimeException("Either app_id or installReference must be non-null.");
        }

        return new MenuSession(
                bean.getUsername(),
                bean.getDomain(),
                bean.getAppId(),
                bean.getInstallReference(),
                bean.getLocale(),
                installService,
                restoreFactory,
                host,
                bean.getOneQuestionPerScreen(),
                bean.getRestoreAs(),
                bean.getPreview()
        );
    }

}
