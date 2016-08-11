package application;

import auth.HqAuth;
import beans.NewFormSessionResponse;
import beans.NotificationMessageBean;
import beans.menus.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.util.cli.*;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import repo.FormSessionRepo;
import repo.MenuSessionRepo;
import repo.SerializableMenuSession;
import screens.FormplayerQueryScreen;
import services.InstallService;
import services.RestoreService;
import session.FormSession;
import session.MenuSession;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

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
        return getNextMenu(menuSession, 0, "", null);
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
            return menuResponseBean;
        }
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

    @ExceptionHandler(Exception.class)
    public String handleError(HttpServletRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception);
        exception.printStackTrace();
        try {
            sendExceptionEmail(exception);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Unable to send email");
        }
        JSONObject errorReturn = new JSONObject();
        errorReturn.put("exception", exception);
        errorReturn.put("url", req.getRequestURL());
        errorReturn.put("status", "error");
        return errorReturn.toString();
    }

    private void sendExceptionEmail(Exception exception) {
        try {
            exceptionMessage.setHtmlMsg(getExceptionEmailBody(exception));
            exceptionMessage.setSubject("Formplayer Menu Exception: " + exception.getMessage());
            exceptionMessage.send();
        } catch(EmailException e){
            // I think we should fail quietly on this
            log.error("Couldn't send exception email: " + e);
        }
    }


    private String getExceptionEmailBody(Exception exception){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String formattedTime = dateFormat.format(new Date());
        String[] stackTrace = ExceptionUtils.getStackTrace(exception).split("\n");
        String stackTraceHTML = StringUtils.replace(
            StringUtils.join(stackTrace, "<br />"), "\t", "&nbsp;&nbsp;&nbsp;"
        );
        return "<html>" +
                "<h3>Message</h3>" +
                "<p>" + exception.getMessage() + "</p>" +
                "<h3>Time</h3>" +
                "<p>" + formattedTime + "<p>" +
                "<h3>Trace</h3>" +
                "<p>" + stackTraceHTML + "</p>" +
                "</html>";
    }
}
