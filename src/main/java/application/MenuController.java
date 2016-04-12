package application;

import beans.InstallRequestBean;
import beans.MenuResponseBean;
import beans.MenuSelectBean;
import beans.SessionBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.commcare.util.cli.EntityScreen;
import org.commcare.util.cli.MenuScreen;
import org.commcare.util.cli.OptionsScreen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import repo.MenuRepo;
import repo.SessionRepo;
import requests.InstallRequest;
import services.InstallService;
import services.RestoreService;
import session.MenuSession;
import util.Constants;

import java.util.HashMap;

/**
 * Created by willpride on 1/12/16.
 */
@Api(value = "Menu Controllers", description = "Operations for navigating CommCare Menus and Cases")
@RestController
@EnableAutoConfiguration
public class MenuController {

    @Autowired
    private MenuRepo menuRepo;

    @Autowired
    private RestoreService restoreService;

    @Autowired
    private InstallService installService;

    @Autowired
    private SessionRepo sessionRepo;

    @ApiOperation(value = "Install the application at the given reference")
    @RequestMapping(value = Constants.URL_INSTALL, method = RequestMethod.POST)
    public SessionBean performInstall(@RequestBody InstallRequestBean installRequestBean) throws Exception {
        InstallRequest installRequest = new InstallRequest(installRequestBean, restoreService, menuRepo, installService);
        return getNextMenu(installRequest.getMenuSession(), true);
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
        MenuSession menuSession = getMenuSession(menuSelectBean.getSessionId());
        boolean redrawing = menuSession.handleInput(menuSelectBean.getSelection());
        menuRepo.save(menuSession.serialize());
        return getNextMenu(menuSession, redrawing);
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
            MenuResponseBean menuResponseBean = new MenuResponseBean();
            menuResponseBean.setSessionId(menuSession.getSessionId());
            // We're looking at a module or form menu
            if(nextScreen instanceof MenuScreen){
                MenuScreen menuScreen = (MenuScreen) nextScreen;
                menuResponseBean.setMenuType(Constants.MENU_MODULE);
                menuResponseBean.setOptions(getMenuRows(menuScreen));
            }
            // We're looking at a case list or detail screen (probably)
            else if (nextScreen instanceof EntityScreen) {
                EntityScreen entityScreen = (EntityScreen) nextScreen;
                menuResponseBean.setMenuType(Constants.MENU_ENTITY);
                menuResponseBean.setOptions(getMenuRows(entityScreen.getCurrentScreen()));
            }
            return menuResponseBean;
        }
    }

    private HashMap<Integer, String> getMenuRows(OptionsScreen nextScreen){
        String[] rows = nextScreen.getOptions();
        HashMap<Integer, String> optionsStrings = new HashMap<Integer, String>();
        for(int i=0; i <rows.length; i++){
            optionsStrings.put(i, rows[i]);
        }
        return optionsStrings;
    }

    private MenuSession getMenuSession(String sessionId) throws Exception {
        return new MenuSession(menuRepo.find(sessionId), restoreService, installService);
    }
}
