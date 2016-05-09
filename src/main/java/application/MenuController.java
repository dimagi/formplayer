package application;

import beans.*;
import beans.menus.CommandListResponseBean;
import beans.menus.EntityListResponse;
import beans.menus.MenuBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.util.cli.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import repo.SessionRepo;
import requests.InstallRequest;
import services.InstallService;
import services.RestoreService;
import session.MenuSession;
import util.Constants;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * Created by willpride on 1/12/16.
 */
@Api(value = "Menu Controllers", description = "Operations for navigating CommCare Menus and Cases")
@RestController
@EnableAutoConfiguration
@CrossOrigin(origins = "http://localhost:8000")
public class MenuController {

    @Autowired
    private RestoreService restoreService;

    @Autowired
    private InstallService installService;

    @Autowired
    private SessionRepo sessionRepo;

    Log log = LogFactory.getLog(MenuController.class);

    @ApiOperation(value = "Install the application at the given reference")
    @RequestMapping(value = Constants.URL_INSTALL, method = RequestMethod.POST)
    public Object performInstall(@RequestBody InstallRequestBean installRequestBean) throws Exception {
        log.info("Received install request: " + installRequestBean);
        InstallRequest installRequest = new InstallRequest(installRequestBean, restoreService, installService);
        Object response = getNextMenu(installRequest.getMenuSession());
        log.info("Returning install response: " + response);
        return response;
    }

    /**
     * Make a a series of menu selections (as above, but can have multiple)
     *
     * @param sessionNavigationBean Give an installation code or path and a set of session selections
     * @return A MenuResponseBean or a NewFormSessionResponse
     * @throws Exception
     */
    @RequestMapping(value = Constants.URL_MENU_NAVIGATION, method = RequestMethod.POST)
    public Object navigateSession(@RequestBody SessionNavigationBean sessionNavigationBean) throws Exception {
        log.info("Navigate session with bean: " + sessionNavigationBean);
        InstallRequest installRequest = new InstallRequest(sessionNavigationBean, restoreService, installService);
        MenuSession menuSession = installRequest.getMenuSession();
        String[] selections = sessionNavigationBean.getSelections();
        Object nextMenu = getNextMenu(menuSession);
        if (selections == null){
            log.info("Selections null, got next menu: " + nextMenu);
            return nextMenu;
        }
        for(String selection: selections) {
            menuSession.handleInput(selection);
            menuSession.setScreen(menuSession.getNextScreen());
        }
        nextMenu = getNextMenu(menuSession);
        log.info("Returning menu: " + nextMenu);
        return nextMenu;
    }



    private Object getNextMenu(MenuSession menuSession) throws Exception {

        OptionsScreen nextScreen;

        // If we were redrawing, remain on the current screen. Otherwise, advance to the next.
        nextScreen = menuSession.getNextScreen();

        // No next menu screen? Start form entry!
        if (nextScreen == null){
            return menuSession.startFormEntry(sessionRepo);
        }
        else{
            MenuBean menuResponseBean;

            // We're looking at a module or form menu
            if(nextScreen instanceof MenuScreen){
                menuResponseBean = generateMenuScreen((MenuScreen) nextScreen);
            }
            // We're looking at a case list or detail screen (probably)
            else if (nextScreen instanceof EntityScreen) {
                menuResponseBean = generateEntityScreen((EntityScreen) nextScreen);
            } else{
                throw new Exception("What screen are we on? " + nextScreen);
            }
            return menuResponseBean;
        }
    }

    private CommandListResponseBean generateMenuScreen(MenuScreen nextScreen){
        return new CommandListResponseBean(nextScreen);
    }

    private EntityListResponse generateEntityScreen(EntityScreen nextScreen){
        return new EntityListResponse(nextScreen);
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
