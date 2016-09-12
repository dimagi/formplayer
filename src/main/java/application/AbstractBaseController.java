package application;

import beans.ExceptionResponseBean;
import beans.NewFormSessionResponse;
import beans.menus.*;
import exceptions.ApplicationConfigException;
import exceptions.FormNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionFrame;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.EntityDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.StackFrameStep;
import org.commcare.util.cli.*;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.engine.models.Session;
import org.javarosa.xpath.XPathException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import repo.FormSessionRepo;
import repo.MenuSessionRepo;
import repo.SerializableMenuSession;
import screens.FormplayerQueryScreen;
import services.InstallService;
import services.RestoreService;
import session.FormSession;
import session.MenuSession;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Base Controller class containing exception handling logic and
 * autowired beans used in both MenuController and FormController
 */
public abstract class AbstractBaseController {

    @Autowired
    protected RestoreService restoreService;

    @Autowired
    protected FormSessionRepo formSessionRepo;

    @Autowired
    protected MenuSessionRepo menuSessionRepo;

    @Autowired
    protected InstallService installService;

    @Autowired
    private HtmlEmail exceptionMessage;

    @Autowired
    protected LockRegistry userLockRegistry;

    @Value("${commcarehq.host}")
    private String hqHost;

    private final Log log = LogFactory.getLog(AbstractBaseController.class);


    public BaseResponseBean resolveFormGetNext(MenuSession menuSession) throws Exception {
        menuSession.getSessionWrapper().syncState();
        if(menuSession.getSessionWrapper().finishExecuteAndPop(menuSession.getSessionWrapper().getEvaluationContext())){
            BaseResponseBean nextMenu = getNextMenu(menuSession);
            menuSessionRepo.save(new SerializableMenuSession(menuSession));
            return nextMenu;
        }
        return null;
    }

    public BaseResponseBean getNextMenu(MenuSession menuSession) throws Exception {
        return getNextMenu(menuSession, 0, "");
    }

    protected BaseResponseBean getNextMenu(MenuSession menuSession,
                                           int offset,
                                           String searchText) throws Exception {
        Screen nextScreen = menuSession.getNextScreen();
        // If the nextScreen is null, that means we are heading into
        // form entry and there isn't a screen title
        if (nextScreen == null) {
            return getNextMenu(menuSession, offset, searchText, null);
        }
        return getNextMenu(menuSession, offset, searchText, new String[] {nextScreen.getScreenTitle()});
    }

    protected BaseResponseBean getNextMenu(MenuSession menuSession,
                                           int offset,
                                           String searchText,
                                           String[] breadcrumbs) throws Exception {
        Screen nextScreen;

        // If we were redrawing, remain on the current screen. Otherwise, advance to the next.
        nextScreen = menuSession.getNextScreen();
        // No next menu screen? Start form entry!
        if (nextScreen == null) {
            if(menuSession.getSessionWrapper().getForm() != null) {
                return generateFormEntryScreen(menuSession);
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
                menuResponseBean = generateEntityScreen((EntityScreen) nextScreen, offset, searchText,
                        menuSession.getId());
            } else if(nextScreen instanceof FormplayerQueryScreen){
                    menuResponseBean = generateQueryScreen((QueryScreen) nextScreen, menuSession.getSessionWrapper());
            } else {
                throw new Exception("Unable to recognize next screen: " + nextScreen);
            }
            menuResponseBean.setBreadcrumbs(breadcrumbs);
            menuResponseBean.setAppId(menuSession.getAppId());
            menuResponseBean.setAppVersion(menuSession.getCommCareVersionString() +
                    ", App Version: " + menuSession.getAppVersion());
            setPersistentCaseTile(menuSession, menuResponseBean);
            return menuResponseBean;
        }
    }

    private void setPersistentCaseTile(MenuSession menuSession, MenuBean menuResponseBean) {
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
            return;
        }

        EntityDatum entityDatum = session.findDatumDefinition(stepToFrame.getId());

        if (entityDatum == null || entityDatum.getPersistentDetail() == null) {
            return;
        }

        Detail persistentDetail = session.getDetail(entityDatum.getPersistentDetail());
        if (persistentDetail == null) {
            return;
        }
        EvaluationContext ec = session.getEvaluationContext();

        TreeReference ref = entityDatum.getEntityFromID(ec, stepToFrame.getValue());
        if (ref == null) {
            return;
        }

        EvaluationContext subContext = new EvaluationContext(ec, ref);

        EntityDetailSubscreen detailSubscreen =
                new EntityDetailSubscreen(0, persistentDetail, subContext, new String[] {"title"});
        menuResponseBean.setPersistentCaseTile(new EntityDetailResponse(detailSubscreen));
    }

    private QueryResponseBean generateQueryScreen(QueryScreen nextScreen, SessionWrapper sessionWrapper) {
        return new QueryResponseBean(nextScreen, sessionWrapper);
    }

    private CommandListResponseBean generateMenuScreen(MenuScreen nextScreen, SessionWrapper session,
                                                       String menuSessionId) {
        return new CommandListResponseBean(nextScreen, session, menuSessionId);
    }

    private EntityListResponse generateEntityScreen(EntityScreen nextScreen, int offset, String searchText,
                                                    String menuSessionId) {
        return new EntityListResponse(nextScreen, offset, searchText, menuSessionId);
    }

    private NewFormSessionResponse generateFormEntryScreen(MenuSession menuSession) throws Exception {
        FormSession formEntrySession = menuSession.getFormEntrySession();
        menuSessionRepo.save(new SerializableMenuSession(menuSession));
        formSessionRepo.save(formEntrySession.serialize());
        return new NewFormSessionResponse(formEntrySession);
    }

    /**
     * Catch all the exceptions that we *do not* want emailed here
     */
    @ExceptionHandler({ApplicationConfigException.class,
            CommCareInstanceInitializer.FixtureInitializationException.class,
            CommCareSessionException.class,
            FormNotFoundException.class})
    @ResponseBody
    public ExceptionResponseBean handleApplicationError(HttpServletRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception);

        return new ExceptionResponseBean(exception.getMessage(), req.getRequestURL().toString());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ExceptionResponseBean handleError(HttpServletRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception);
        exception.printStackTrace();
        try {
            sendExceptionEmail(req, exception);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Unable to send email");
        }
        return new ExceptionResponseBean(exception.getMessage(), req.getRequestURL().toString());
    }

    private void sendExceptionEmail(HttpServletRequest req, Exception exception) {
        try {
            exceptionMessage.setHtmlMsg(getExceptionEmailBody(req, exception));
            exceptionMessage.setSubject("Formplayer Exception: " + exception.getMessage());
            exceptionMessage.send();
        } catch(EmailException e){
            // I think we should fail quietly on this
            log.error("Couldn't send exception email: " + e);
        }
    }


    private String getExceptionEmailBody(HttpServletRequest req, Exception exception){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String formattedTime = dateFormat.format(new Date());
        String[] stackTrace = ExceptionUtils.getStackTrace(exception).split("\n");
        String stackTraceHTML = StringUtils.replace(
            StringUtils.join(stackTrace, "<br />"), "\t", "&nbsp;&nbsp;&nbsp;"
        );
        return "<h3>Message</h3>" +
                "<p>" + exception.getMessage() + "</p>" +
                "<h3>Request URI</h3>" +
                "<p>" + req.getRequestURI() + "</p>" +
                "<h3>Host</h3>" +
                "<p>" + hqHost + "</p>" +
                "<h3>Time</h3>" +
                "<p>" + formattedTime + "</p>" +
                "<h3>Trace</h3>" +
                "<p>" + stackTraceHTML + "</p>";
    }

    protected Lock getLockAndBlock(String username){
        Lock lock = userLockRegistry.obtain(username);
        obtainLock(lock);
        return lock;
    }

    protected boolean obtainLock(Lock lock) {
        try {
            return lock.tryLock(60, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            return obtainLock(lock);
        }
    }
}
