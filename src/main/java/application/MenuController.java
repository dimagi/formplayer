package application;

import annotations.UserLock;
import auth.BasicAuth;
import auth.DjangoAuth;
import auth.HqAuth;
import beans.InstallRequestBean;
import beans.NotificationMessageBean;
import beans.SessionNavigationBean;
import beans.menus.BaseResponseBean;
import beans.menus.UpdateRequestBean;
import exceptions.MenuNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.util.screen.CommCareSessionException;
import org.commcare.util.screen.Screen;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import screens.FormplayerQueryScreen;
import screens.FormplayerSyncScreen;
import session.MenuSession;
import util.ApplicationUtils;
import util.Constants;
import util.SessionUtils;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.concurrent.locks.Lock;

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
    @UserLock
    public BaseResponseBean installRequest(@RequestBody InstallRequestBean installRequestBean,
                                           @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        BaseResponseBean response = getNextMenu(performInstall(installRequestBean, authToken));
        return response;
    }

    @ApiOperation(value = "Update the application at the given reference")
    @RequestMapping(value = Constants.URL_UPDATE, method = RequestMethod.POST)
    @UserLock
    public BaseResponseBean updateRequest(@RequestBody UpdateRequestBean updateRequestBean,
                                           @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        BaseResponseBean response = getNextMenu(performUpdate(updateRequestBean, authToken));
        return response;
    }

    /**
     * Make a a series of menu selections (as above, but can have multiple)
     *
     * @param sessionNavigationBean Give an installation code or path and a set of session selections
     * @param authToken             The Django session id auth token
     * @return A MenuBean or a NewFormResponse
     * @throws Exception
     */
    @RequestMapping(value = Constants.URL_MENU_NAVIGATION, method = RequestMethod.POST)
    @UserLock
    public BaseResponseBean navigateSessionWithAuth(@RequestBody SessionNavigationBean sessionNavigationBean,
                                          @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        MenuSession menuSession;
        DjangoAuth auth = new DjangoAuth(authToken);
        restoreFactory.configure(sessionNavigationBean, auth);
        String menuSessionId = sessionNavigationBean.getMenuSessionId();
        if (menuSessionId != null && !"".equals(menuSessionId)) {
            try {
                menuSession = new MenuSession(menuSessionRepo.findOneWrapped(menuSessionId),
                        installService, restoreFactory, auth, host);
                menuSession.getSessionWrapper().syncState();
            } catch(MenuNotFoundException e) {
                return new BaseResponseBean(null, e.getMessage(), true, true);
            }
        } else {
            // If we have a preview command, load that up
            if(sessionNavigationBean.getPreviewCommand() != null){
                // When previewing, clear and reinstall DBs to get newest version
                // Big TODO: app updates
                ApplicationUtils.deleteApplicationDbs(sessionNavigationBean.getDomain(), sessionNavigationBean.getUsername(),
                        sessionNavigationBean.getAppId());
                menuSession = performInstall(sessionNavigationBean, authToken);
                try {
                    menuSession.getSessionWrapper().setCommand(sessionNavigationBean.getPreviewCommand());
                    menuSession.updateScreen();
                } catch(ArrayIndexOutOfBoundsException e) {
                    throw new RuntimeException("Couldn't get entries from preview command "
                            + sessionNavigationBean.getPreviewCommand() + ". If this error persists" +
                            " please report a bug to the CommCareHQ Team.");
                }
            } else {
                menuSession = performInstall(sessionNavigationBean, authToken);
            }
        }
        String[] selections = sessionNavigationBean.getSelections();
        BaseResponseBean nextMenu;
        if (selections == null) {
            nextMenu = getNextMenu(menuSession,
                    sessionNavigationBean.getOffset(),
                    sessionNavigationBean.getSearchText());
            return nextMenu;
        }

        String[] titles = new String[selections.length + 1];
        titles[0] = SessionUtils.getAppTitle();
        NotificationMessageBean notificationMessageBean = new NotificationMessageBean();
        for (int i = 1; i <= selections.length; i++) {
            String selection = selections[i - 1];
            boolean gotNextScreen = menuSession.handleInput(selection);
            if (!gotNextScreen) {
                notificationMessageBean = new NotificationMessageBean(
                        "Overflowed selections with selection " + selection + " at index " + i, (true));
                break;
            }
            titles[i] = SessionUtils.getBestTitle(menuSession.getSessionWrapper());
            Screen nextScreen = menuSession.getNextScreen();

            checkDoQuery(nextScreen,
                    menuSession,
                    notificationMessageBean,
                    sessionNavigationBean.getQueryDictionary(),
                    auth);

            BaseResponseBean syncResponse = checkDoSync(nextScreen,
                    menuSession,
                    notificationMessageBean,
                    auth);
            if (syncResponse != null) {
                return syncResponse;
            }
        }
        nextMenu = getNextMenu(menuSession,
                sessionNavigationBean.getOffset(),
                sessionNavigationBean.getSearchText(),
                titles);
        if (nextMenu != null) {
            nextMenu.setNotification(notificationMessageBean);
            log.info("Returning menu: " + nextMenu);
            return nextMenu;
        } else {
            return new BaseResponseBean(null, "Got null menu, redirecting to home screen.", false, true);
        }
    }

    /**
     * If we've encountered a QueryScreen and have a QueryDictionary, do the query
     * and update the session, screen, and notification message accordingly.
     *
     * Will do nothing if this wasn't a query screen.
     */
    private void checkDoQuery(Screen nextScreen,
                              MenuSession menuSession,
                              NotificationMessageBean notificationMessageBean,
                              Hashtable<String, String> quertDictionary,
                              DjangoAuth auth) throws CommCareSessionException {
        if(nextScreen instanceof FormplayerQueryScreen && quertDictionary != null){
            log.info("Formplayer doing query with dictionary " + quertDictionary);
            notificationMessageBean = doQuery((FormplayerQueryScreen) nextScreen,
                    quertDictionary,
                    auth);
            menuSession.updateScreen();
            nextScreen = menuSession.getNextScreen();
            log.info("Next screen after query: " + nextScreen);
        }
    }

    protected NotificationMessageBean doQuery(FormplayerQueryScreen nextScreen,
                                              Hashtable<String, String> queryDictionary,
                                              HqAuth auth) {
        nextScreen.answerPrompts(queryDictionary);
        InputStream responseStream = nextScreen.makeQueryRequestReturnStream();
        boolean success = nextScreen.processSuccess(responseStream);
        if(success){
            return new NotificationMessageBean("Successfully queried server", false);
        } else{
            return new NotificationMessageBean("Query failed with message " + nextScreen.getCurrentMessage(), false);
        }
    }

    /**
     * If we've encountered a sync screen, performing the sync and update the notification
     * and screen accordingly. After a sync, we can either pop another menu/form to begin
     * or just return to the app menu.
     *
     * Return null if this wasn't a sync screen.
     */
    private BaseResponseBean checkDoSync(Screen nextScreen,
                             MenuSession menuSession,
                             NotificationMessageBean notificationMessageBean,
                             DjangoAuth auth) throws Exception {
        // If we've encountered a SyncScreen, perform the sync
        if(nextScreen instanceof FormplayerSyncScreen){
            notificationMessageBean = doSync(
                    (FormplayerSyncScreen)nextScreen,
                    auth);

            BaseResponseBean postSyncResponse = resolveFormGetNext(menuSession);
            if(postSyncResponse != null){
                // If not null, we have a form or menu to redirect to
                postSyncResponse.setNotification(notificationMessageBean);
                return postSyncResponse;
            } else{
                // Otherwise, return use to the app root
                postSyncResponse = new BaseResponseBean(null, "Redirecting after sync", false, true);
                postSyncResponse.setNotification(notificationMessageBean);
                return postSyncResponse;
            }
        }
        return null;
    }

    private NotificationMessageBean doSync(FormplayerSyncScreen screen, DjangoAuth djangoAuth) throws Exception {
        ResponseEntity<String> responseEntity = screen.launchRemoteSync(djangoAuth);
        if(responseEntity == null){
            return new NotificationMessageBean("Session error, expected sync block but didn't get one.", true);
        }
        if(responseEntity.getStatusCode().is2xxSuccessful()){
            return new NotificationMessageBean("Case claim successful", false);
        } else{
            return new NotificationMessageBean("Case claim failed with message: " + responseEntity.getBody(), true);
        }
    }


    private MenuSession performInstall(InstallRequestBean bean, String authToken) throws Exception {
        restoreFactory.configure(bean, new DjangoAuth(authToken));
        if ((bean.getAppId() == null || "".equals(bean.getAppId())) &&
                bean.getInstallReference() == null || "".equals(bean.getInstallReference())) {
            throw new RuntimeException("Either app_id or installReference must be non-null.");
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
                bean.getInstallReference(), bean.getLocale(), installService, restoreFactory, auth, host,
                        bean.getOneQuestionPerScreen(), bean.getRestoreAs());
    }

    private MenuSession performUpdate(UpdateRequestBean updateRequestBean, String authToken) throws Exception {
        MenuSession currentSession = performInstall(updateRequestBean, authToken);
        currentSession.updateApp(updateRequestBean.getUpdateMode());
        return currentSession;
    }
}
