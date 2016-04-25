package application;

import beans.InstallRequestBean;
import beans.MenuResponseBean;
import beans.MenuSelectBean;
import beans.SessionBean;
import beans.menus.CommandListResponseBean;
import beans.menus.EntityDetailResponseBean;
import beans.menus.EntityListResponseBean;
import beans.menus.MenuSessionBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.util.cli.*;
import org.javarosa.engine.models.Session;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import repo.MenuRepo;
import repo.SessionRepo;
import requests.InstallRequest;
import services.InstallService;
import services.RestoreService;
import session.MenuSession;
import util.Constants;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * Created by willpride on 1/12/16.
 */
@Api(value = "Menu Controllers", description = "Operations for navigating CommCare Menus and Cases")
@RestController
@EnableAutoConfiguration
@CrossOrigin(origins = "http://localhost:8000")
public class MenuController {

    @Autowired
    private MenuRepo menuRepo;

    @Autowired
    private RestoreService restoreService;

    @Autowired
    private InstallService installService;

    @Autowired
    private SessionRepo sessionRepo;

    Log log = LogFactory.getLog(MenuController.class);

    @ApiOperation(value = "Install the application at the given reference")
    @RequestMapping(value = Constants.URL_INSTALL, method = RequestMethod.POST)
    public SessionBean performInstall(@RequestBody InstallRequestBean installRequestBean) throws Exception {
        log.info("Received install request: " + installRequestBean);
        InstallRequest installRequest = new InstallRequest(installRequestBean, restoreService, menuRepo, installService);
        SessionBean response = getNextMenu(installRequest.getMenuSession(), true);
        response.setSequenceId(1);
        log.info("Returning install response: " + response);
        return response;
    }

    /**
     * Make a menu selection, return a new Menu or a Form to display. This form will load a MenuSession object
     * from persistence, make the selection, and update the record. If the selection began form entry, this will
     * also create a new FormSession object.
     *
     * @param menuSelectBean Give the selection to be made on the current MenuSession
     *                       (could be a module, form, or case selection)
     * @return A MenuResponseBean or a NewFormSessionResponse
     * @throws Exception
     */
    @ApiOperation(value = "Make the given menu selection and return the next set of options, or a form to play.")
    @RequestMapping(value = Constants.URL_MENU_SELECT, method = RequestMethod.POST)
    public SessionBean selectMenu(@RequestBody MenuSelectBean menuSelectBean) throws Exception {
        log.info("Select Menu with bean: " + menuSelectBean);
        MenuSession menuSession = getMenuSession(menuSelectBean.getSessionId());
        boolean redrawing = menuSession.handleInput(menuSelectBean.getSelection());
        menuRepo.save(menuSession.serialize());
        SessionBean nextMenu = getNextMenu(menuSession, redrawing);
        log.info("Returning menu: " + nextMenu);
        return nextMenu;
    }

    private SessionBean getNextMenu(MenuSession menuSession, boolean redrawing) throws Exception {

        OptionsScreen nextScreen;

        // If we were redrawing, remain on the current screen. Otherwise, advance to the next.
        if(redrawing){
            nextScreen = menuSession.getCurrentScreen();
        }else{
            nextScreen = menuSession.getNextScreen();
        }

        // No next menu screen? Start form entry!
        if (nextScreen == null){
            return menuSession.startFormEntry(sessionRepo);
        }
        else{
            MenuSessionBean menuResponseBean = new MenuSessionBean();

            // We're looking at a module or form menu
            if(nextScreen instanceof MenuScreen){
                menuResponseBean = generateMenuScreen((MenuScreen) nextScreen);
            }
            // We're looking at a case list or detail screen (probably)
            else if (nextScreen instanceof EntityScreen) {
                if(((EntityScreen) nextScreen).getCurrentScreen() instanceof EntityListSubscreen) {
                    menuResponseBean = generateEntityListScreen((EntityScreen) nextScreen);
                } else if (((EntityScreen) nextScreen).getCurrentScreen() instanceof EntityDetailSubscreen){
                    menuResponseBean = generateEntityDetailScreen((EntityScreen) nextScreen);
                }
            }
            menuResponseBean.setSessionId(menuSession.getSessionId());
            return menuResponseBean;
        }
    }

    private CommandListResponseBean generateMenuScreen(MenuScreen nextScreen){
        return new CommandListResponseBean(nextScreen);
    }

    private EntityListResponseBean generateEntityListScreen(EntityScreen nextScreen){
        return new EntityListResponseBean(nextScreen);
    }

    private EntityDetailResponseBean generateEntityDetailScreen(EntityScreen nextScreen){
        return new EntityDetailResponseBean(nextScreen);
    }

    private MenuSession getMenuSession(String sessionId) throws Exception {
        return new MenuSession(menuRepo.find(sessionId), restoreService, installService);
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
