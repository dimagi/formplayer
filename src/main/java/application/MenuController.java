package application;

import auth.BasicAuth;
import auth.DjangoAuth;
import auth.HqAuth;
import beans.InstallRequestBean;
import beans.SessionNavigationBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import repo.SerializableMenuSession;
import session.MenuSession;
import util.Constants;
import util.SessionUtils;

import java.util.Arrays;

/**
 * Controller (API endpoint) containing all session navigation functionality.
 * This includes module, form, case, and session (incomplete form) selection.
 */
@Api(value = "Menu Controllers", description = "Operations for navigating CommCare Menus and Cases")
@RestController
@EnableAutoConfiguration
public class MenuController extends AbstractBaseController{

    @Value("${commcarehq.host}")
    private String host;

    private final Log log = LogFactory.getLog(MenuController.class);

    @ApiOperation(value = "Install the application at the given reference")
    @RequestMapping(value = Constants.URL_INSTALL, method = RequestMethod.POST)
    public Object installRequest(@RequestBody InstallRequestBean installRequestBean,
                                 @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("Received install request: " + installRequestBean);
        Object response = getNextMenu(performInstall(installRequestBean, authToken));
        log.info("Returning install response: " + response);
        return response;
    }

    /**
     * Make a a series of menu selections (as above, but can have multiple)
     *
     * @param sessionNavigationBean Give an installation code or path and a set of session selections
     * @param authToken             The Django session id auth token
     * @return A MenuBean or a NewFormSessionResponse
     * @throws Exception
     */


    @RequestMapping(value = Constants.URL_MENU_NAVIGATION, method = RequestMethod.POST)
    public Object navigateSessionWithAuth(@RequestBody SessionNavigationBean sessionNavigationBean,
                                          @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("Navigate session with bean: " + sessionNavigationBean + " and authtoken: " + authToken);
        MenuSession menuSession;
        String menuSessionId = sessionNavigationBean.getMenuSessionId();
        if(menuSessionId != null && !"".equals(menuSessionId)) {
            menuSession = new MenuSession(menuSessionRepo.findOne(menuSessionId),
                    installService, restoreService, new DjangoAuth(authToken));
            menuSession.getSessionWrapper().syncState();
        } else{
            menuSession = performInstall(sessionNavigationBean, authToken);
        }
        String[] selections = sessionNavigationBean.getSelections();
        Object nextMenu = getNextMenu(menuSession);
        if (selections == null) {
            return nextMenu;
        }

        String[] titles = new String[selections.length + 1];
        titles[0] = menuSession.getNextScreen().getScreenTitle();
        for(int i=1; i <= selections.length; i++) {
            String selection = selections[i - 1];
            boolean gotNextScreen = menuSession.handleInput(selection);
            if(!gotNextScreen) {
                // If we overflowed selections, just return the last real screen.
                // TODO: Once case claim is merge, set notification here.
                log.info("Couldn't get next screen with selection " + selection +
                        " of selections " + Arrays.toString(selections));
                break;
            }
            titles[i] = SessionUtils.getBestTitle(menuSession.getSessionWrapper());
        }
        nextMenu = getNextMenu(menuSession, sessionNavigationBean.getOffset(),
                sessionNavigationBean.getSearchText(), titles);

        menuSessionRepo.save(new SerializableMenuSession(menuSession));
        log.info("Returning menu: " + nextMenu);
        return nextMenu;
    }


    private MenuSession performInstall(InstallRequestBean bean, String authToken) throws Exception {
        if ((bean.getAppId() == null || "".equals(bean.getAppId())) &&
                bean.getInstallReference() == null || "".equals(bean.getInstallReference())) {
            throw new RuntimeException("Either app_id or install_reference must be non-null.");
        }

        HqAuth auth;
        if (authToken != null && !authToken.trim().equals("")) {
            auth = new DjangoAuth(authToken);
        } else {
            String password = bean.getPassword();
            if (password == null || "".equals(password.trim())) {
                throw new RuntimeException("Either authToken or password must be non-null");
            }
            auth = new BasicAuth(bean.getUsername(), bean.getDomain(), host, password);
        }

        return new MenuSession(bean.getUsername(), bean.getDomain(), bean.getAppId(),
                bean.getInstallReference(), bean.getLocale(), installService, restoreService, auth);
    }
}
