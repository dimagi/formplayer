package application;

import annotations.AppInstall;
import annotations.UserLock;
import annotations.UserRestore;
import auth.BasicAuth;
import auth.DjangoAuth;
import auth.HqAuth;
import beans.InstallRequestBean;
import beans.NewFormResponse;
import beans.NotificationMessageBean;
import beans.SessionNavigationBean;
import beans.menus.BaseResponseBean;
import beans.menus.EntityDetailListResponse;
import beans.menus.UpdateRequestBean;
import exceptions.FormNotFoundException;
import exceptions.MenuNotFoundException;
import hq.CaseAPIs;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.util.screen.CommCareSessionException;
import org.commcare.util.screen.EntityScreen;
import org.commcare.util.screen.Screen;
import org.javarosa.core.model.instance.TreeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.SystemPublicMetrics;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import screens.FormplayerQueryScreen;
import screens.FormplayerSyncScreen;
import services.QueryRequester;
import services.SyncRequester;
import session.FormSession;
import session.MenuSession;
import util.ApplicationUtils;
import util.Constants;
import util.SessionUtils;
import util.UserUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

    @Autowired
    private QueryRequester queryRequester;

    @Autowired
    private SyncRequester syncRequester;

    private final Log log = LogFactory.getLog(MenuController.class);

    @ApiOperation(value = "Install the application at the given reference")
    @RequestMapping(value = Constants.URL_INSTALL, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    @AppInstall
    public BaseResponseBean installRequest(@RequestBody InstallRequestBean installRequestBean,
                                           @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        return getNextMenu(performInstall(installRequestBean, authToken));
    }

    @ApiOperation(value = "Update the application at the given reference")
    @RequestMapping(value = Constants.URL_UPDATE, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    @AppInstall
    public BaseResponseBean updateRequest(@RequestBody UpdateRequestBean updateRequestBean,
                                           @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        MenuSession updatedSession = performUpdate(updateRequestBean, authToken);
        if (updateRequestBean.getSessionId() != null) {
            // Try restoring the old session, fail gracefully.
            try {
                FormSession oldSession = new FormSession(formSessionRepo.findOneWrapped(updateRequestBean.getSessionId()), restoreFactory);
                updatedSession.reloadSession(oldSession);
                return new NewFormResponse(oldSession);
            } catch(FormNotFoundException e) {
                log.info("FormSession with id " + updateRequestBean.getSessionId() + " not found, returning root");
            } catch(Exception e) {
                log.info("FormSession with id " + updateRequestBean.getSessionId()
                        + " failed to load with exception " + e);
            }
        }
        return getNextMenu(updatedSession);
    }

    @RequestMapping(value = Constants.URL_GET_DETAILS, method = RequestMethod.POST)
    @UserLock
    @UserRestore
    @AppInstall
    public EntityDetailListResponse getDetails(@RequestBody SessionNavigationBean sessionNavigationBean,
                                               @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        MenuSession menuSession;
        HqAuth auth = getAuthHeaders(
                sessionNavigationBean.getDomain(),
                sessionNavigationBean.getUsername(),
                authToken
        );
        try {
            menuSession = getMenuSessionFromBean(sessionNavigationBean, authToken);
        } catch (MenuNotFoundException e) {
            return null;
        }

        String[] selections = sessionNavigationBean.getSelections();
        String[] commitSelections = new String[selections.length - 1];
        String detailSelection = selections[selections.length - 1];
        System.arraycopy(selections, 0, commitSelections, 0, selections.length - 1);

        advanceSessionWithSelections(menuSession,
                commitSelections,
                auth,
                detailSelection,
                sessionNavigationBean.getQueryDictionary(),
                sessionNavigationBean.getOffset(),
                sessionNavigationBean.getSearchText()
        );
        Screen currentScreen = menuSession.getNextScreen();
        if (!(currentScreen instanceof EntityScreen)) {
            throw new RuntimeException("Tried to get details while not on a case list.");
        }
        EntityScreen entityScreen = (EntityScreen) currentScreen;
        TreeReference reference = entityScreen.resolveTreeReference(detailSelection);

        if (reference == null) {
            throw new RuntimeException("Could not find case with ID " + detailSelection);
        }

        EntityDetailListResponse response = new EntityDetailListResponse(
                entityScreen,
                menuSession.getSessionWrapper().getEvaluationContext(),
                reference
        );
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
    @UserRestore
    @AppInstall
    public BaseResponseBean navigateSessionWithAuth(@RequestBody SessionNavigationBean sessionNavigationBean,
                                          @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {
        MenuSession menuSession;
        HqAuth auth = getAuthHeaders(
                sessionNavigationBean.getDomain(),
                sessionNavigationBean.getUsername(),
                authToken
        );
        menuSession = getMenuSessionFromBean(sessionNavigationBean, authToken);
        String[] selections = sessionNavigationBean.getSelections();
        return advanceSessionWithSelections(
                menuSession,
                selections,
                auth,
                null,
                sessionNavigationBean.getQueryDictionary(),
                sessionNavigationBean.getOffset(),
                sessionNavigationBean.getSearchText()
        );
    }

    private MenuSession getMenuSessionFromBean(SessionNavigationBean sessionNavigationBean, String authToken) throws Exception {
        MenuSession menuSession = null;
        HqAuth auth = getAuthHeaders(
                sessionNavigationBean.getDomain(),
                sessionNavigationBean.getUsername(),
                authToken
        );
        String menuSessionId = sessionNavigationBean.getMenuSessionId();
        if (menuSessionId != null && !"".equals(menuSessionId)) {
            menuSession = new MenuSession(
                    menuSessionRepo.findOneWrapped(menuSessionId),
                    installService,
                    restoreFactory,
                    auth,
                    host
            );
            menuSession.getSessionWrapper().syncState();
        } else {
            // If we have a preview command, load that up
            if(sessionNavigationBean.getPreviewCommand() != null){
                menuSession = handlePreviewCommand(sessionNavigationBean, authToken);
            } else {
                menuSession = performInstall(sessionNavigationBean, authToken);
            }
        }
        return menuSession;
    }

    /**
     * Advances the session based on the selections.
     *
     * @param menuSession
     * @param selections - Selections are either an integer index into a list of modules
     * or a case id indicating the case selected for a case detail.
     *
     * An example selection would be ["0", "2", "6c5d91e9-61a2-4264-97f3-5d68636ff316"]
     *
     * This would mean select the 0th menu, then the 2nd menu, then the case with the id 6c5d91e9-61a2-4264-97f3-5d68636ff316.
     *
     * @param auth
     * @param detailSelection - If requesting a case detail will be a case id, else null. When the case id is given
     * it is used to short circuit the normal TreeReference calculation by inserting a predicate that
     * is [@case_id = <detailSelection>].
     * @param queryDictionary
     * @param offset
     * @param searchText
     *  */
    private BaseResponseBean advanceSessionWithSelections(MenuSession menuSession,
                                              String[] selections,
                                              HqAuth auth,
                                              String detailSelection,
                                              Hashtable<String, String> queryDictionary,
                                              int offset,
                                              String searchText) throws Exception {
        BaseResponseBean nextMenu;
        // If we have no selections, we're are the root screen.
        if (selections == null) {
            nextMenu = getNextMenu(
                    menuSession,
                    offset,
                    searchText
            );
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

            notificationMessageBean = checkDoQuery(
                    nextScreen,
                    menuSession,
                    queryDictionary
            );

            BaseResponseBean syncResponse = checkDoSync(nextScreen,
                    menuSession,
                    notificationMessageBean,
                    auth,
                    selections);
            if (syncResponse != null) {
                return syncResponse;
            }
        }
        nextMenu = getNextMenu(
                menuSession,
                detailSelection,
                offset,
                searchText,
                titles
        );
        if (nextMenu != null) {
            nextMenu.setNotification(notificationMessageBean);
            log.info("Returning menu: " + nextMenu);
            return nextMenu;
        } else {
            return new BaseResponseBean(null, "Got null menu, redirecting to home screen.", false, true);
        }
    }

    private MenuSession handlePreviewCommand(SessionNavigationBean sessionNavigationBean, String authToken) throws Exception{
        MenuSession menuSession;
        // When previewing, clear and reinstall DBs to get newest version
        // Big TODO: app updates
        ApplicationUtils.deleteApplicationDbs(
                sessionNavigationBean.getDomain(),
                sessionNavigationBean.getUsername(),
                sessionNavigationBean.getRestoreAs(),
                sessionNavigationBean.getAppId()
        );
        menuSession = performInstall(sessionNavigationBean, authToken);
        try {
            menuSession.getSessionWrapper().setCommand(sessionNavigationBean.getPreviewCommand());
            menuSession.updateScreen();
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Couldn't get entries from preview command "
                    + sessionNavigationBean.getPreviewCommand() + ". If this error persists" +
                    " please report a bug to the CommCareHQ Team.");
        }
        return menuSession;
    }

    /**
     * If we've encountered a QueryScreen and have a QueryDictionary, do the query
     * and update the session, screen, and notification message accordingly.
     *
     * Will do nothing if this wasn't a query screen.
     */
    private NotificationMessageBean checkDoQuery(Screen nextScreen,
                              MenuSession menuSession,
                              Hashtable<String, String> queryDictionary) throws CommCareSessionException {
        if(nextScreen instanceof FormplayerQueryScreen && queryDictionary != null){
            log.info("Formplayer doing query with dictionary " + queryDictionary);
            NotificationMessageBean notificationMessageBean = doQuery((FormplayerQueryScreen) nextScreen,
                    queryDictionary);
            menuSession.updateScreen();
            nextScreen = menuSession.getNextScreen();
            log.info("Next screen after query: " + nextScreen);
            return notificationMessageBean;
        }
        return null;
    }

    protected NotificationMessageBean doQuery(FormplayerQueryScreen nextScreen,
                                              Hashtable<String, String> queryDictionary) {
        nextScreen.answerPrompts(queryDictionary);
        String responseString = queryRequester.makeQueryRequest(nextScreen.getUriString(), nextScreen.getAuthHeaders());
        boolean success = nextScreen.processSuccess(new ByteArrayInputStream(responseString.getBytes(StandardCharsets.UTF_8)));
        if(success){
            return new NotificationMessageBean("Successfully queried server", false);
        } else{
            return new NotificationMessageBean("Query failed with message " + nextScreen.getCurrentMessage(), true);
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
                             HqAuth auth,
                             String[] selections) throws Exception {
        // If we've encountered a SyncScreen, perform the sync
        if(nextScreen instanceof FormplayerSyncScreen){
            notificationMessageBean = doSync(
                    (FormplayerSyncScreen) nextScreen,
                    auth
            );

            BaseResponseBean postSyncResponse = resolveFormGetNext(menuSession);
            if(postSyncResponse != null){
                // If not null, we have a form or menu to redirect to
                postSyncResponse.setNotification(notificationMessageBean);
                postSyncResponse.setSelections(trimCaseClaimSelections(selections));
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

    private String[] trimCaseClaimSelections(String[] selections) {
        String actionSelections = selections[selections.length - 2];
        if (!actionSelections.contains("action")) {
            log.error(String.format("Selections %s did not contain expected action at position %s.",
                    Arrays.toString(selections),
                    selections[selections.length - 2]));
            return selections;
        }
        String[] newSelections = new String[selections.length - 1];
        System.arraycopy(selections, 0, newSelections, 0, selections.length - 2);
        newSelections[selections.length - 2] = selections[selections.length - 1];
        return newSelections;
    }

    private NotificationMessageBean doSync(FormplayerSyncScreen screen, HqAuth auth) throws Exception {
        ResponseEntity<String> responseEntity = syncRequester.makeSyncRequest(screen.getUrl(),
                screen.getBuiltQuery(),
                auth.getAuthHeaders());
        if(responseEntity == null){
            return new NotificationMessageBean("Session error, expected sync block but didn't get one.", true);
        }
        if(responseEntity.getStatusCode().is2xxSuccessful()){
            CaseAPIs.forceRestore(restoreFactory);
            return new NotificationMessageBean("Case claim successful.", false);
        } else{
            return new NotificationMessageBean(
                    String.format("Case claim failed. Message: %s", responseEntity.getBody()), true);
        }
    }


    private MenuSession performInstall(InstallRequestBean bean, String authToken) throws Exception {
        HqAuth auth = getAuthHeaders(
                bean.getDomain(),
                bean.getUsername(),
                authToken
        );
        if ((bean.getAppId() == null || "".equals(bean.getAppId())) &&
                bean.getInstallReference() == null || "".equals(bean.getInstallReference())) {
            throw new RuntimeException("Either app_id or installReference must be non-null.");
        }

        return new MenuSession(
                bean.getUsername(),
                bean.getDomain(),
                bean.getAppId(),
                bean.getInstallReference(),
                bean.getLocale(),
                installService,
                restoreFactory,
                auth,
                host,
                bean.getOneQuestionPerScreen(),
                bean.getRestoreAs()
        );
    }

    private MenuSession performUpdate(UpdateRequestBean updateRequestBean, String authToken) throws Exception {
        MenuSession currentSession = performInstall(updateRequestBean, authToken);
        currentSession.updateApp(updateRequestBean.getUpdateMode());
        return currentSession;
    }
}
