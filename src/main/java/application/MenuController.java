package application;

import auth.BasicAuth;
import auth.DjangoAuth;
import auth.HqAuth;
import beans.NotificationMessageBean;
import beans.InstallRequestBean;
import beans.SessionNavigationBean;
import beans.menus.BaseResponseBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.util.cli.Screen;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repo.SerializableMenuSession;
import screens.FormplayerQueryScreen;
import screens.FormplayerSyncScreen;
import session.MenuSession;
import util.Constants;
import util.SessionUtils;

import java.util.Hashtable;

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
    public BaseResponseBean installRequest(@RequestBody InstallRequestBean installRequestBean,
                                           @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        log.info("Received install request: " + installRequestBean);
        BaseResponseBean response = getNextMenu(performInstall(installRequestBean, authToken));
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
        DjangoAuth auth = new DjangoAuth(authToken);
        String menuSessionId = sessionNavigationBean.getMenuSessionId();
        if(menuSessionId != null && !"".equals(menuSessionId)) {
            menuSession = new MenuSession(menuSessionRepo.findOne(menuSessionId),
                    installService, restoreService, auth);
            menuSession.getSessionWrapper().syncState();
        } else{
            menuSession = performInstall(sessionNavigationBean, authToken);
        }
        String[] selections = sessionNavigationBean.getSelections();
        BaseResponseBean nextMenu = getNextMenu(menuSession);
        if (selections == null) {
            return nextMenu;
        }

        String[] titles = new String[selections.length + 1];
        titles[0] = menuSession.getNextScreen().getScreenTitle();
        NotificationMessageBean notificationMessageBean = new NotificationMessageBean();
        for(int i=1; i <= selections.length; i++) {
            String selection = selections[i - 1];
            menuSession.handleInput(selection);
            titles[i] = SessionUtils.getBestTitle(menuSession.getSessionWrapper());
            Screen nextScreen = menuSession.getNextScreen();
            // If we've encountered a QueryScreen and have a QueryDictionary, do the query
            if(nextScreen instanceof FormplayerQueryScreen && sessionNavigationBean.getQueryDictionary() != null){
                log.info("Formplayer doing query with dictionary " + sessionNavigationBean.getQueryDictionary());
                notificationMessageBean = doQuery((FormplayerQueryScreen) nextScreen,
                        sessionNavigationBean.getQueryDictionary(),
                        new DjangoAuth(authToken));
                menuSession.updateScreen();
                nextScreen = menuSession.getNextScreen();
                log.info("Next screen after query: " + nextScreen);
            }
            // If we've encountered a SyncScreen, perform the sync
            if(nextScreen instanceof FormplayerSyncScreen){
                notificationMessageBean = doSync(
                        (FormplayerSyncScreen)nextScreen,
                        new DjangoAuth(authToken));

                BaseResponseBean postSyncResponse = resolveFormGetNext(menuSession);
                if(postSyncResponse != null){
                    // If not null, we have a form or menu to redirect to
                    postSyncResponse.setNotification(notificationMessageBean);
                    return postSyncResponse;
                } else{
                    // Otherwise, return use to the app root
                    menuSession = performInstall(sessionNavigationBean, authToken);
                    postSyncResponse = getNextMenu(menuSession);
                    postSyncResponse.setNotification(notificationMessageBean);
                    return postSyncResponse;
                }
            }
        }
        nextMenu = getNextMenu(menuSession,
                sessionNavigationBean.getOffset(),
                sessionNavigationBean.getSearchText(),
                titles);
        if(nextMenu != null){
            nextMenu.setNotification(notificationMessageBean);
            menuSessionRepo.save(new SerializableMenuSession(menuSession));
            log.info("Returning menu: " + nextMenu);
            return nextMenu;
        } else{
            return new BaseResponseBean(null, "Redirecting after case claim", false, true);
        }
    }

    private NotificationMessageBean doSync(FormplayerSyncScreen screen, DjangoAuth djangoAuth) throws Exception {
        ResponseEntity<String> responseEntity = screen.launchRemoteSync(djangoAuth);
        if(responseEntity.getStatusCode().is2xxSuccessful()){
            return new NotificationMessageBean("Case claim successful", false);
        } else{
            return new NotificationMessageBean("Case claim failed with message: " + responseEntity.getBody(), true);
        }
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
