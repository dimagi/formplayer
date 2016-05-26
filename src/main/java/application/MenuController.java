package application;

import auth.BasicAuth;
import auth.DjangoAuth;
import auth.HqAuth;
import beans.*;
import beans.menus.CommandListResponseBean;
import beans.menus.EntityListResponse;
import beans.menus.MenuBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.util.cli.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import repo.SessionRepo;
import services.InstallService;
import services.RestoreService;
import session.FormSession;
import session.MenuSession;
import util.Constants;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by willpride on 1/12/16.
 */
@Api(value = "Menu Controllers", description = "Operations for navigating CommCare Menus and Cases")
@RestController
@EnableAutoConfiguration
public class MenuController {

    @Value("${commcarehq.host}")
    private String host;

    @Autowired
    private RestoreService restoreService;

    @Autowired
    private InstallService installService;

    @Autowired
    private SessionRepo sessionRepo;

    private final Log log = LogFactory.getLog(MenuController.class);

    @ApiOperation(value = "Install the application at the given reference")
    @RequestMapping(value = Constants.URL_INSTALL, method = RequestMethod.POST)
    public Object installRequest(@RequestBody InstallRequestBean installRequestBean,
                                 @CookieValue("sessionid") String authToken) throws Exception {
        log.info("Received install request: " + installRequestBean);
        Object response = getNextMenu(performInstall(installRequestBean, authToken));
        log.info("Returning install response: " + response);
        return response;
    }

    /**
     * Make a a series of menu selections (as above, but can have multiple)
     *
     * @param sessionNavigationBean Give an installation code or path and a set of session selections
     * @param authToken The Django session id auth token
     * @return A MenuBean or a NewFormSessionResponse
     * @throws Exception
     */


    @RequestMapping(value = Constants.URL_MENU_NAVIGATION, method = RequestMethod.POST)
    public Object navigateSessionWithAuth(@RequestBody SessionNavigationBean sessionNavigationBean,
                                          @CookieValue("sessionid") String authToken) throws Exception {
        log.info("Navigate session with bean: " + sessionNavigationBean + " and authtoken: " + authToken);
        MenuSession menuSession = performInstall(sessionNavigationBean, authToken);
        String[] selections = sessionNavigationBean.getSelections();
        Object nextMenu = getNextMenu(menuSession);
        if (selections == null){
            log.info("Selections null, got next menu: " + nextMenu);
            return nextMenu;
        }
        for(String selection: selections) {
            menuSession.handleInput(selection);
        }
        nextMenu = getNextMenu(menuSession, sessionNavigationBean.getOffset());
        log.info("Returning menu: " + nextMenu);
        return nextMenu;
    }

    private MenuSession performInstall(InstallRequestBean bean, String authToken) throws Exception {
        if((bean.getAppId() == null || "".equals(bean.getAppId())) &&
                bean.getInstallReference() == null || "".equals(bean.getInstallReference())){
            throw new RuntimeException("Either app_id or install_reference must be non-null.");
        }

        HqAuth auth;
        if(authToken != null && !authToken.trim().equals("")){
            auth = new DjangoAuth(authToken);
        } else{
            String password = bean.getPassword();
            if(password == null || "".equals(password.trim())){
                throw new RuntimeException("Either authToken or password must be non-null");
            }
            auth = new BasicAuth(bean.getUsername(), bean.getDomain(), "commcarehq.org", password);
        }

        return new MenuSession(bean.getUsername(), bean.getDomain(), bean.getAppId(),
                bean.getInstallReference(), installService, restoreService, auth);
    }

    private Object getNextMenu(MenuSession menuSession) throws Exception {
        return getNextMenu(menuSession, 0);
    }

    private Object getNextMenu(MenuSession menuSession, int offset) throws Exception {

        Screen nextScreen;

        // If we were redrawing, remain on the current screen. Otherwise, advance to the next.
        nextScreen = menuSession.getNextScreen();

        // No next menu screen? Start form entry!
        if (nextScreen == null){
            return generateFormEntryScreen(menuSession);
        }
        else{
            MenuBean menuResponseBean;

            // We're looking at a module or form menu
            if(nextScreen instanceof MenuScreen){
                menuResponseBean = generateMenuScreen((MenuScreen) nextScreen, menuSession.getSessionWrapper());
            }
            // We're looking at a case list or detail screen (probably)
            else if (nextScreen instanceof EntityScreen) {
                menuResponseBean = generateEntityScreen((EntityScreen) nextScreen, offset);
            } else{
                throw new Exception("Unable to recognize next screen: " + nextScreen);
            }
            return menuResponseBean;
        }
    }

    private CommandListResponseBean generateMenuScreen(MenuScreen nextScreen, SessionWrapper session){
        return new CommandListResponseBean(nextScreen, session);
    }

    private EntityListResponse generateEntityScreen(EntityScreen nextScreen, int offset){
        return new EntityListResponse(nextScreen, offset);
    }

    private NewFormSessionResponse generateFormEntryScreen(MenuSession menuSession) throws Exception {
        FormSession formEntrySession = menuSession.getFormEntrySession();
        sessionRepo.save(formEntrySession.serialize());
        log.info("Start form entry with session: " + formEntrySession);
        return new NewFormSessionResponse(formEntrySession);
    }

    @ExceptionHandler(Exception.class)
    public String handleError(HttpServletRequest req, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception);
        exception.printStackTrace();
        JSONObject errorReturn = new JSONObject();
        errorReturn.put("exception", exception);
        errorReturn.put("url", req.getRequestURL());
        errorReturn.put("status", "error");
        return errorReturn.toString();
    }
}
