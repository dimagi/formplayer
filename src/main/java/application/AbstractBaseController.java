package application;

import auth.DjangoAuth;
import auth.HqAuth;
import auth.TokenAuth;
import beans.NewFormResponse;
import beans.exceptions.ExceptionResponseBean;
import beans.exceptions.HTMLExceptionResponseBean;
import beans.exceptions.RetryExceptionResponseBean;
import beans.menus.*;
import com.getsentry.raven.Raven;
import com.getsentry.raven.event.Event;
import com.getsentry.raven.event.EventBuilder;
import com.getsentry.raven.event.interfaces.ExceptionInterface;
import com.timgroup.statsd.StatsDClient;
import exceptions.*;
import hq.models.PostgresUser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.modern.models.RecordTooLargeException;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.modern.util.Pair;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.util.screen.*;
import org.commcare.util.screen.CommCareSessionException;
import org.commcare.util.screen.EntityScreen;
import org.commcare.util.screen.MenuScreen;
import org.commcare.util.screen.QueryScreen;
import org.commcare.util.screen.Screen;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import repo.FormSessionRepo;
import repo.MenuSessionRepo;
import repo.SerializableMenuSession;
import repo.impl.PostgresUserRepo;
import screens.FormplayerQueryScreen;
import services.FormplayerStorageFactory;
import services.InstallService;
import services.NewFormResponseFactory;
import services.RestoreFactory;
import session.FormSession;
import session.MenuSession;
import util.*;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * Base Controller class containing exception handling logic and
 * autowired beans used in both MenuController and FormController
 */
public abstract class AbstractBaseController {

    @Value("${commcarehq.host}")
    protected String host;

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
    protected FormplayerRaven raven;

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
            return getNextMenu(menuSession);
        }
        return null;
    }

    public BaseResponseBean getNextMenu(MenuSession menuSession) throws Exception {
        return getNextMenu(menuSession, 0, "");
    }

    protected HqAuth getAuthHeaders(String domain, String username, String sessionToken) {
        HqAuth auth;
        if (UserUtils.isAnonymous(domain, username)) {
            PostgresUser postgresUser = postgresUserRepo.getUserByUsername(username);
            auth = new TokenAuth(postgresUser.getAuthToken());
        } else {
            auth = new DjangoAuth(sessionToken);
        }
        return auth;
    }

    protected BaseResponseBean getNextMenu(MenuSession menuSession,
                                           int offset,
                                           String searchText) throws Exception {
        Screen nextScreen = menuSession.getNextScreen();
        // If the nextScreen is null, that means we are heading into
        // form entry and there isn't a screen title
        if (nextScreen == null) {
            return getNextMenu(menuSession, null, offset, searchText);
        }
        return getNextMenu(menuSession, null, offset, searchText);
    }

    protected BaseResponseBean getNextMenu(MenuSession menuSession,
                                           String detailSelection,
                                           int offset,
                                           String searchText) throws Exception {
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
                        menuSession.getId()
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

    protected EntityDetailResponse getPersistentDetail(MenuSession menuSession) {
        Pair<TreeReference, Detail> pair = getDetail(menuSession, false);
        if (pair == null) {
            return null;
        }
        EvaluationContext ec = new EvaluationContext(menuSession.getSessionWrapper().getEvaluationContext(), pair.first);
        return new EntityDetailResponse(pair.second, ec);
    }

    protected EntityDetailListResponse getInlineDetail(MenuSession menuSession) {
        Pair<TreeReference, Detail> pair = getDetail(menuSession, true);
        if (pair == null) {
            return null;
        }
        EvaluationContext ec = menuSession.getSessionWrapper().getEvaluationContext();
        return new EntityDetailListResponse(pair.second.getDetails(),
                ec,
                pair.first);
    }

    protected Pair<TreeReference, Detail> getDetail(MenuSession menuSession, boolean inline) {

        SessionWrapper session = menuSession.getSessionWrapper();

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

        EvaluationContext ec = session.getEvaluationContext();

        TreeReference ref = entityDatum.getEntityFromID(ec, stepToFrame.getValue());
        if (ref == null) {
            return null;
        }

        return new Pair(ref, persistentDetail);

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
                                                    String menuSessionId) {
        return new EntityListResponse(nextScreen, detailSelection, offset, searchText, menuSessionId);
    }

    private NewFormResponse generateFormEntryScreen(MenuSession menuSession) throws Exception {
        FormSession formEntrySession = menuSession.getFormEntrySession();
        menuSessionRepo.save(new SerializableMenuSession(menuSession));
        NewFormResponse response = new NewFormResponse(formEntrySession);
        formSessionRepo.save(formEntrySession.serialize());
        return response;
    }

    protected MenuSession getMenuSession(String domain, String username, String menuSessionId, String authToken) throws Exception {
        MenuSession menuSession = null;
        HqAuth auth = getAuthHeaders(
                domain,
                username,
                authToken
        );

        menuSession = new MenuSession(
                menuSessionRepo.findOneWrapped(menuSessionId),
                installService,
                restoreFactory,
                auth,
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
        raven.sendRavenException(request, exception, Event.Level.INFO);
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

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ExceptionResponseBean handleError(FormplayerHttpRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception);
        incrementDatadogCounter(Constants.DATADOG_ERRORS_CRASH, req);
        exception.printStackTrace();
        raven.sendRavenException(req, exception);
        return new ExceptionResponseBean(exception.getMessage(), req.getRequestURL().toString());
    }

    private void incrementDatadogCounter(String metric, FormplayerHttpRequest req) {
        String user = "unknown";
        String domain = "unknown";
        if (req.getCouchUser() != null) {
            user = req.getCouchUser().getUsername();
        }
        if (req.getDomain() != null) {
            domain = req.getDomain();
        }
        datadogStatsDClient.increment(
                metric,
                "domain:" + domain,
                "user:" + user,
                "request:" + req.getRequestURL()
        );
    }
}
